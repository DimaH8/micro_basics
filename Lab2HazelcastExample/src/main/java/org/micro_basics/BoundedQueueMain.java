package org.micro_basics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.collection.IQueue;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;


import java.io.Serializable;
import java.util.Objects;

public class BoundedQueueMain {



    public static void main( String[] args ) throws Exception {
        String address = System.getenv("HAZELCAST_IP");
        String type = System.getenv("TYPE_WORK");

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(address);
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

        System.out.println( "Starting BoundedQueueMain for " + address);
        System.out.println( "TYPE = " + type);

        IQueue<Integer> queue = hz.getQueue( "default" );
        if (Objects.equals(type, "read")) {
            while (true) {
                int item = queue.take();
                System.out.println("Consumed: " + item);
                if (item == -1) {
                    queue.put(-1);
                    break;
                }
                Thread.sleep(5000);
            }
            System.out.println("Consumer Finished!");
        }
        else {
            String data = System.getenv("DATA");
            int number = Integer.parseInt(data);
            for ( int k = 1; k < number; k++ ) {
                queue.put( k );
                System.out.println( "Producing: " + k );
                Thread.sleep(1000);
            }
            queue.put( -1 );
            System.out.println( "Producer Finished!" );
        }
    }

}
