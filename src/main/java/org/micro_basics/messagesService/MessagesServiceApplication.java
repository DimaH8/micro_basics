package org.micro_basics.messagesService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


import java.io.IOException;
import java.io.OutputStream;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hazelcast.collection.IQueue;

import com.hazelcast.core.HazelcastInstance;
import org.micro_basics.common.ServiceBase;
import org.micro_basics.common.Utils;

public class MessagesServiceApplication {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("SERVICE_PORT"));
        String name = System.getenv("SERVICE_NAME");
        String consulUrl = System.getenv("CONSUL_URL");

        System.out.println("Service name is " + name);
        System.out.println("Service port is " + port);

        MessagesServiceController service = new MessagesServiceController(name, port, consulUrl);
        service.run();

        Utils.waitDockerSignal();

        service.shutdown();
    }
}

class MessagesServiceController extends ServiceBase {
    private static Queue<String> serviceQueue = new ConcurrentLinkedQueue<String>();
    private Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    String msgQueueName;

    MessagesServiceController(String name, int port, String consulUrl)  {
        super(name, port, consulUrl);
        setHttpHandler("/", new RequestHandler());

        msgQueueName = consulConnection.getConfigValueAsString("app/config/hazelcast/queue/name");
        System.out.println("msgQueueName = " + msgQueueName);

        thread = new Thread(() -> {
            System.out.println("Hazelcast Distributed Queue reader thread started");
            while (running.get()) {
                IQueue<String> queue = hzInstance.getQueue(msgQueueName);
                try {
                    String msg = queue.take();
                    System.out.println("Read from Hazelcast Distributed Queue: " + msg);
                    serviceQueue.add(msg);
                } catch (InterruptedException e) {
                    System.out.println("Could not read from Hazelcast Distributed Queue. Error: " + e);
                }
            }
        });
        thread.start();
        System.out.println("Service " + serviceId + " is created");
    }

    public void shutdown() {
        // finish thread
        running.set(false);
        shutdown();
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.out.println("Cannot thread.join");
        }
    }
    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String responseData = null;
            int statusCode;
            String method = httpExchange.getRequestMethod();
            System.out.println("Received " + method + " request");
            if(method.equals("GET")) {
                statusCode = 200;
                String allMsgs = String.join(",", serviceQueue);
                System.out.println("All msgs: " + allMsgs);
                responseData = allMsgs;
            } else {
                statusCode = 405;
                responseData = "Method Not Allowed ";
            }
            httpExchange.sendResponseHeaders(statusCode, responseData.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseData.getBytes());
            os.close();
        }
    }
}
