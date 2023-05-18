package org.micro_basics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.UUID;

import java.util.Map;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        // Press Alt+Enter with your caret at the highlighted text to see how
        // IntelliJ IDEA suggests fixing it.
        System.out.println("Lab2 Hazelcast!");

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("127.0.0.1:5701");
        HazelcastInstance hzInstance = HazelcastClient.newHazelcastClient(clientConfig);

        Map<Integer, String> hzMap = hzInstance.getMap( "default" );
        for (int i = 0; i < 1000; i++) {
            UUID uuid=UUID.randomUUID();
            hzMap.put(i, uuid.toString());
        }
        System.out.println("1000 test keys loaded");

        hzInstance.shutdown();
    }
}