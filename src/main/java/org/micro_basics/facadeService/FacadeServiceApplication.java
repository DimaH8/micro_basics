package org.micro_basics.facadeService;

import java.io.IOException;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FacadeServiceApplication {

    public static void main(String[] args) {
        try {
            FacadeServiceController server = new FacadeServiceController();
            System.out.println("Facade service is running");
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class FacadeServiceController {

    private static String[] loggingServiceIPs = {"http://192.168.0.112:8002/", "http://192.168.0.122:8002/", "http://192.168.0.132:8002/"};

    private static String getLoggingServiceIP() {
        int randomNum = ThreadLocalRandom.current().nextInt(0, loggingServiceIPs.length);
        return loggingServiceIPs[randomNum];
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

                    System.out.println("Send GET to messagesService");
                    String responseFromMessagesService = sendRequest("http://192.168.0.103:8003/");
                    if (responseFromMessagesService != null) {
                        System.out.println("messagesService response:" + responseFromMessagesService);
                        responseData = responseFromLoggingService + " " + responseFromMessagesService;
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
    void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
        HttpContext context = server.createContext("/");
        context.setHandler(new RequestHandler());
        server.start();
    }
}