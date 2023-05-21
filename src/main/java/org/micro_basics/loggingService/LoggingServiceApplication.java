package org.micro_basics.loggingService;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoggingServiceApplication {
    public static void main(String[] args) {
        try {
            LoggingServiceController server = new LoggingServiceController();
            System.out.println("Logging service is running");
            server.run();
            System.out.println("Logging service finished");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class LoggingServiceController {
    private static String hazelcastIP = null;

    private static void addToMap(UUID uuid, String msg)
    {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress(hazelcastIP);
        HazelcastInstance hzInstance = HazelcastClient.newHazelcastClient(config);
        System.out.println("HazelcastClient created");
        Map<String, String> messages = hzInstance.getMap( "facadeServiceMessages" );
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        messages.put(uuid.toString(), msg);
        hzInstance.shutdown();
    }

    private static String getMapValues()
    {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress(hazelcastIP);
        HazelcastInstance hzInstance = HazelcastClient.newHazelcastClient(config);
        System.out.println("HazelcastClient created");
        Map<String, String> messages = hzInstance.getMap( "facadeServiceMessages" );
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        String values = String.join(",", messages.values());
        hzInstance.shutdown();
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
    void run() throws IOException {
        hazelcastIP = System.getenv("HAZELCAST_IP");
        System.out.println("Hazelcast address is " + hazelcastIP);

        System.out.println("Start web-server");
        HttpServer server = HttpServer.create(new InetSocketAddress(8002),0);
        HttpContext context = server.createContext("/");
        context.setHandler(new LoggingServiceController.RequestHandler());
        server.start();
    }
}
