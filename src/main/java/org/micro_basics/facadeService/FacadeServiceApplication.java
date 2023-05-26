package org.micro_basics.facadeService;

import java.io.IOException;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.IQueue;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.micro_basics.consul.ConsulConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FacadeServiceApplication {
    private static HazelcastInstance hzInstance;
    static ConsulConnection consulConnection;

    public static String getHazelcastAddress() {
        String allHazelcasts = consulConnection.getConfigValueAsString("app/config/all-hazelcast-instances");
        String[] hzAddresses = allHazelcasts.split(",");
        int randomNum = ThreadLocalRandom.current().nextInt(0, hzAddresses.length);
        return hzAddresses[randomNum];
    }

    public static void main(String[] args) {
        String myIp = ConsulConnection.getHostIpAddress();
        System.out.println("Facade-Service IP is " + myIp);
        String hostname = ConsulConnection.getHostname();
        System.out.println("Facade-Service hostname is " + hostname);
        String serviceName = System.getenv("SERVICE_NAME");
        String serviceId = System.getenv("SERVICE_ID");
        consulConnection = new ConsulConnection(serviceId, serviceName, myIp);
        String hazelcastIP = getHazelcastAddress();
        System.out.println("Hazelcast address is " + hazelcastIP);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastIP);
        hzInstance = HazelcastClient.newHazelcastClient(clientConfig);

        try {
            FacadeServiceController server = new FacadeServiceController(hzInstance);
            int port = consulConnection.getConfigValueAsInt("app/config/facade-service/port");
            System.out.println("Facade service is running on port " + port);
            server.run(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class FacadeServiceController {

    private static HazelcastInstance hzInstance;

    FacadeServiceController(HazelcastInstance hz) {
        hzInstance = hz;
    }
    private static String getMessagesServiceIP() {
        String allMessagesSrvs = FacadeServiceApplication.consulConnection.getConfigValueAsString("app/config/all-messages-services");
        String[] msgSrvsAddresses = allMessagesSrvs.split(",");

        int randomNum = ThreadLocalRandom.current().nextInt(0, msgSrvsAddresses.length);
        return "http://" + msgSrvsAddresses[randomNum] + "/";
    }
    private static String getLoggingServiceIP() {
        String allLoggingSrvs = FacadeServiceApplication.consulConnection.getConfigValueAsString("app/config/all-logging-services");
        String[] loggingSrvsAddresses = allLoggingSrvs.split(",");

        int randomNum = ThreadLocalRandom.current().nextInt(0, loggingSrvsAddresses.length);
        return "http://" + loggingSrvsAddresses[randomNum] + "/";
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
    private static String sendRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            System.out.println("Could not send GET request: " + url);
            System.out.println("ERROR is : " + e);
            return null;
        }
        System.out.println("URL = " + uri);
        System.out.println("response code = " + response.statusCode());
        return response.body();
    }

    private static void sendMsgToQueue(String msg) {
        IQueue<String> queue = hzInstance.getQueue("default");
        try {
            queue.put(msg);
            System.out.println("Added to Hazelcast Queue: " + msg);
        } catch (InterruptedException e) {
            System.out.println("Could not add msg to Hazelcast Queue. Error is " + e);
        }
    }
    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String responseData = null;
            int statusCode;
            String method = httpExchange.getRequestMethod();
            System.out.println("Received " + method + " request");
            if(method.equals("POST")) {
                statusCode = 200;
                UUID uuid = UUID.randomUUID();
                String msg = getPostData(httpExchange);
                System.out.println("POST data: " + msg);
                sendMsgToQueue(msg);
                String resultPair = uuid + "," + msg;
                String ip = getLoggingServiceIP();
                System.out.println("Send POST to loggingService (" + ip + ") with data: " + resultPair);
                HttpClient httpClient = HttpClient.newHttpClient();
                URI url = URI.create(ip);
                HttpRequest request = HttpRequest.newBuilder(url).POST(HttpRequest.BodyPublishers.ofString(resultPair)).build();
                HttpResponse<String> response;
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("response code = " + response.statusCode());
                responseData = "Everything is ok ";
            }else if(method.equals("GET")) {
                statusCode = 200;
                String ip = getLoggingServiceIP();
                System.out.println("Send GET to loggingService " + ip);
                String responseFromLoggingService = sendRequest(ip);
                if (responseFromLoggingService != null) {
                    System.out.println("loggingService response:" + responseFromLoggingService);

                    System.out.println("Send GET to messagesService " + ip);
                    ip = getMessagesServiceIP();
                    String responseFromMessagesService = sendRequest(ip);
                    if (responseFromMessagesService != null) {
                        System.out.println("messagesService response:" + responseFromMessagesService);
                        responseData = responseFromLoggingService + "  |  " + responseFromMessagesService;
                    } else {
                        statusCode = 500;
                        responseData = "MessagesService Error";
                    }
                } else {
                    statusCode = 500;
                    responseData = "LoggingService Error";
                }
            } else {
                statusCode = 405;
                responseData = "Method Not Allowed ";
            }
            responseData += "\n";
            httpExchange.sendResponseHeaders(statusCode, responseData.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseData.getBytes());
            os.close();
        }
    }
    void run(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port),0);
        HttpContext context = server.createContext("/");
        context.setHandler(new RequestHandler());
        server.start();
    }
}
