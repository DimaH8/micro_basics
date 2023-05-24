package org.micro_basics.messagesService;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.IQueue;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class MessagesServiceApplication {
    private static HazelcastInstance hzInstance;

    public static void main(String[] args) {
        String hazelcastIP = System.getenv("HAZELCAST_IP");
        System.out.println("Hazelcast address is " + hazelcastIP);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastIP);
        hzInstance = HazelcastClient.newHazelcastClient(clientConfig);

        try {
            MessagesServiceController server = new MessagesServiceController(hzInstance);
            System.out.println("Messages service is running");
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MessagesServiceController {
    private static HazelcastInstance hzInstance;
    private static Queue<String> serviceQueue = new ConcurrentLinkedQueue<String>();
    private Thread thread;

    MessagesServiceController(HazelcastInstance hz) {
        hzInstance = hz;
        thread = new Thread(() -> {
            System.out.println("Hazelcast Distributed Queue reader thread started");
            while (true) {
                IQueue<String> queue = hzInstance.getQueue("default");
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
    void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8003),0);
        HttpContext context = server.createContext("/");
        context.setHandler(new MessagesServiceController.RequestHandler());
        server.start();
    }
}
