package org.micro_basics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

public class LockInit {
    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress("192.168.0.11:5701");
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

        IMap<String, Value> map = hz.getMap("default");
        String key = "1";
        map.put(key, new Value());
        hz.shutdown();
    }

}