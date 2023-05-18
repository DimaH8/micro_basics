package org.micro_basics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.Serializable;

public class PessimisticUpdateMember {
    public static void main( String[] args ) throws Exception {
        String address = System.getenv("HAZELCAST_IP");
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(address);
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

        IMap<String, Value> map = hz.getMap( "default" );
        String key = "1";
        map.put( key, new Value() );
        System.out.println( "Starting PessimisticUpdateMember for" + address );
        for ( int k = 0; k < 1000; k++ ) {
            map.lock( key );
            try {
                Value value = map.get( key );
                Thread.sleep( 10 );
                value.amount++;
                map.put( key, value );
            } finally {
                map.unlock( key );
            }
        }
        System.out.println( "Finished PessimisticUpdateMember! Result = " + map.get( key ).amount );
    }

    static class Value implements Serializable {
        public int amount;
    }
}