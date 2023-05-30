package org.micro_basics.loggingService;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.micro_basics.common.ConsulConnection;
import org.micro_basics.common.ServiceBase;
import org.micro_basics.common.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LoggingServiceApplication {


    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("SERVICE_PORT"));
        String name = System.getenv("SERVICE_NAME");
        String id = System.getenv("SERVICE_ID");
        String consulUrl = System.getenv("CONSUL_URL");

        System.out.println("Service name is " + name);
        System.out.println("Service id is " + id);
        System.out.println("Service port is " + port);

        LoggingServiceController service = new LoggingServiceController(name, id, port, consulUrl);
        service.run();

        Utils.waitDockerSignal();

        service.shutdown();
    }
}

class LoggingServiceController  extends ServiceBase {
    private static HazelcastInstance hzInstance;

    LoggingServiceController(String name, String id, int port, String consulUrl)  {
        super(name, id, port, consulUrl);

        String hazelcastIP = getHazelcastAddress();
        System.out.println("Hazelcast address is " + hazelcastIP);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastIP);
        hzInstance = HazelcastClient.newHazelcastClient(clientConfig);
        System.out.println("Service " + serviceId + " is created");
    }
    void run() {
        HttpContext context = httpServer.createContext("/");
        context.setHandler(new RequestHandler());
        httpServer.start();
        System.out.println("Service " + serviceId + " started");
    }

    void shutdown() {
        httpServer.stop(0);
        hzInstance.shutdown();
        consulConnection.close();
        System.out.println("Service " + serviceId + " finished");
    }

    private static void addToMap(UUID uuid, String msg) {
        Map<String, String> messages = hzInstance.getMap( "facadeServiceMessages" );
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        messages.put(uuid.toString(), msg);
    }

    private static String getMapValues() {
        Map<String, String> messages = hzInstance.getMap( "facadeServiceMessages" );
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        String values = String.join(",", messages.values());
        return values;
    }

    private static String getPostData(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream ios = exchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }
        return sb.toString();
    }
    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String responseData;
            int statusCode;
            String method = httpExchange.getRequestMethod();
            System.out.println("Received " + method + " request");
            if(method.equals("POST")) {
                statusCode = 200;
                String bodyRequest = getPostData(httpExchange);
                int index = bodyRequest.indexOf(",");
                if (index == -1) {
                    throw new RuntimeException("Unknown exception");
                }
                UUID uuid = UUID.fromString(bodyRequest.substring(0,index));
                String msg = bodyRequest.substring(index + 1);
                addToMap(uuid, msg);
                System.out.println("UUID: " + uuid + " msg: " + msg);
                responseData = "Everything is ok ";
            }else if(method.equals("GET")) {
                statusCode = 200;
                responseData = getMapValues();
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
