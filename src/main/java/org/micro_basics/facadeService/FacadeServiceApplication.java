package org.micro_basics.facadeService;

import java.io.IOException;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;

import org.micro_basics.common.ServiceBase;
import org.micro_basics.common.Utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FacadeServiceApplication {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("SERVICE_PORT"));
        String name = System.getenv("SERVICE_NAME");
        String id = System.getenv("SERVICE_ID");
        String consulUrl = System.getenv("CONSUL_URL");

        System.out.println("Service name is " + name);
        System.out.println("Service id is " + id);
        System.out.println("Service port is " + port);

        FacadeServiceController service = new FacadeServiceController(name, id, port, consulUrl);
        service.run();

        Utils.waitDockerSignal();

        service.shutdown();
    }
}
class FacadeServiceController extends ServiceBase {
    String msgQueueName;
    FacadeServiceController(String name, String id, int port, String consulUrl)  {
        super(name, id, port, consulUrl);
        setHttpHandler("/", new RequestHandler());
        System.out.println("Service " + serviceId + " is created");
        msgQueueName = consulConnection.getConfigValueAsString("app/config/hazelcast/queue/name");
        System.out.println("msgQueueName = " + msgQueueName);
    }

    private String getMessagesServiceIP() {
        List<String> msgServices = consulConnection.getServices("messages-service");
        int randomNum = ThreadLocalRandom.current().nextInt(0, msgServices.size());
        return "http://" + msgServices.get(randomNum) + "/";
    }
    private String getLoggingServiceIP() {
        List<String> loggingServices = consulConnection.getServices("logging-service");
        int randomNum = ThreadLocalRandom.current().nextInt(0, loggingServices.size());
        return "http://" + loggingServices.get(randomNum) + "/";
    }

    private String sendRequest(String url) {
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

    private void sendMsgToQueue(String msg) {
        IQueue<String> queue = hzInstance.getQueue(msgQueueName);
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
}
