package org.micro_basics.loggingService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.micro_basics.common.ServiceBase;
import org.micro_basics.common.Utils;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Map;
import java.util.UUID;

public class LoggingServiceApplication {


    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("SERVICE_PORT"));
        String name = System.getenv("SERVICE_NAME");
        String consulUrl = System.getenv("CONSUL_URL");

        System.out.println("Service name is " + name);
        System.out.println("Service port is " + port);

        LoggingServiceController service = new LoggingServiceController(name, port, consulUrl);
        service.run();

        Utils.waitDockerSignal();

        service.shutdown();
    }
}

class LoggingServiceController  extends ServiceBase {
    String mapName;
    LoggingServiceController(String name, int port, String consulUrl)  {
        super(name, port, consulUrl);
        setHttpHandler("/", new RequestHandler());
        System.out.println("Service " + serviceId + " is created");

        mapName = consulConnection.getConfigValueAsString("app/config/hazelcast/map/name");
        System.out.println("mapName = " + mapName);
    }

    private void addToMap(UUID uuid, String msg) {
        Map<String, String> messages = hzInstance.getMap(mapName);
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        messages.put(uuid.toString(), msg);
    }

    private String getMapValues() {
        Map<String, String> messages = hzInstance.getMap(mapName);
        System.out.println("HazelcastClient facadeServiceMessages map ok");
        String values = String.join(",", messages.values());
        return values;
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
