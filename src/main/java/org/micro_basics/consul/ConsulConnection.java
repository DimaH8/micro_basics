package org.micro_basics.consul;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;

import java.util.Arrays;
import java.util.Collections;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConsulConnection {
    private Consul consulClient;
    private KeyValueClient consulKVClient;

    public ConsulConnection(String id, String name, String ip) {
        consulClient = Consul.builder().withUrl("http://192.168.0.10:8500").build();
        AgentClient agentClient = consulClient.agentClient();

        Registration service = ImmutableRegistration.builder()
                .id(id)
                .name(name)
                .address(ip)
                //.port(8000)
                .tags(Collections.singletonList(id))
                .meta(Collections.singletonMap("version", "1.0"))
                .build();

        agentClient.register(service);

        consulKVClient = consulClient.keyValueClient();
    }

    public String getConfigValueAsString(String key) {
        String value = consulKVClient.getValueAsString(key).get();
        System.out.println("Consul KV (string): " + key + " : " + value);
        return value;
    }

    public int getConfigValueAsInt(String key) {
        String sValue = consulKVClient.getValueAsString(key).get();
        int value = Integer.parseInt(sValue);
        System.out.println("Consul KV (int): " + key + " : " + value);
        return value;
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

    public static String getHostname() {
        try {
            InetAddress host = InetAddress.getLocalHost();
            return host.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }
}
