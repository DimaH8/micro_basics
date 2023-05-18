package org.micro_basics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
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

        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress("127.0.0.1" + ":" + 5701);
        HazelcastInstance hzInstance = HazelcastClient.newHazelcastClient(config);

        Map<Integer, String> hzMap = hzInstance.getMap( "uuids-test" );
        for (int i = 0; i < 1000; i++) {
            UUID uuid=UUID.randomUUID();
            hzMap.put(i, uuid.toString());
        }
        System.out.println("1000 test keys loaded");
        hzInstance.shutdown();
    }
}