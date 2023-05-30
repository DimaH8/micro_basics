package org.micro_basics.common;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

public class ServiceBase {
    protected int servicePort;
    protected String serviceName;
    protected String serviceId;

    protected HttpServer httpServer;
    protected ConsulConnection consulConnection;

    public ServiceBase(String name, String id, int port, String consulUrl) {
        servicePort = port;
        serviceName = name;
        serviceId = id;

        String myIp = getHostIpAddress();
        System.out.println("IP address is " + myIp);

        consulConnection = new ConsulConnection(consulUrl, serviceId, serviceName, myIp);
        try {
            httpServer = HttpServer.create(new InetSocketAddress(servicePort),0);
            System.out.println("HttpServer created on port " + servicePort);
        } catch (IOException e) {
            System.out.println("HttpServer.create failed");
            throw new RuntimeException(e);
        }
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
}
