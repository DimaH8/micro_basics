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

public class OptimisticMember {
    public static void main( String[] args ) throws Exception {
        String address = System.getenv("HAZELCAST_IP");

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(address);
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

        IMap<String, Value> map = hz.getMap( "default" );
        String key = "1";
        System.out.println( "Starting OptimisticMember for " + address);
        for ( int k = 0; k < 1000; k++ ) {
            if ( k % 10 == 0 ) System.out.println( "At: " + k );
            for (; ; ) {
                Value oldValue = map.get( key );
                Value newValue = new Value( oldValue );
                Thread.sleep( 10 );
                newValue.amount++;
                if ( map.replace( key, oldValue, newValue ) )
                    break;
            }
        }
        System.out.println( "Finished OptimisticMember! Result = " + map.get( key ).amount );
    }
}