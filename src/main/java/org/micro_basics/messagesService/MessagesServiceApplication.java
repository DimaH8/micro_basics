package org.micro_basics.messagesService;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MessagesServiceApplication {
    public static void main(String[] args) {
        try {
            MessagesServiceController server = new MessagesServiceController();
            System.out.println("Messages service is running");
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MessagesServiceController {
    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String responseData = null;
            int statusCode;
            String method = httpExchange.getRequestMethod();
            System.out.println("Received " + method + " request");
            if(method.equals("GET")) {
                statusCode = 200;
                responseData = "not implemented yet ";
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
