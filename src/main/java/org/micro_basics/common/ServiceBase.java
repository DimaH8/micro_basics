package org.micro_basics.common;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ServiceBase {
    protected int servicePort;
    protected String serviceName;
    protected String serviceId;

    protected HttpServer httpServer;
    protected HazelcastInstance hzInstance;
    protected ConsulConnection consulConnection;

    public ServiceBase(String name, int port, String consulUrl) {
        servicePort = port;
        serviceName = name;
        serviceId = name + "-" + UUID.randomUUID();
        System.out.println("Service ID: " + serviceId);
        String myIp = getHostIpAddress();
        System.out.println("IP address: " + myIp);

        try {
            httpServer = HttpServer.create(new InetSocketAddress(servicePort),0);
            System.out.println("HttpServer created on port " + servicePort);
        } catch (IOException e) {
            System.out.println("HttpServer.create failed");
            throw new RuntimeException(e);
        }

        consulConnection = new ConsulConnection(consulUrl, serviceId, serviceName, myIp, port);

        String hazelcastIP = getHazelcastAddress();
        System.out.println("Hazelcast address is " + hazelcastIP);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastIP);
        hzInstance = HazelcastClient.newHazelcastClient(clientConfig);
    }

    public void run() {
        httpServer.start();
        System.out.println("Service " + serviceId + " started");
    }

    public void shutdown() {
        httpServer.stop(0);
        hzInstance.shutdown();
        consulConnection.close();
        System.out.println("Service " + serviceId + " finished");
    }

    protected void setHttpHandler(String path, HttpHandler httpHandler) {
        HttpContext context = httpServer.createContext(path);
        context.setHandler(httpHandler);
    }

    protected String getHazelcastAddress() {
        String allHazelcasts = consulConnection.getConfigValueAsString("app/config/all-hazelcast-instances");
        String[] hzAddresses = allHazelcasts.split(",");
        int randomNum = ThreadLocalRandom.current().nextInt(0, hzAddresses.length);
        return hzAddresses[randomNum];
    }

    public static String getHostIpAddress() {
        try {
            InetAddress host = InetAddress.getLocalHost();
            return host.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

    protected static String getPostData(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream ios = exchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }
        return sb.toString();
    }
}
