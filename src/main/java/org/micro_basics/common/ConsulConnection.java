package org.micro_basics.common;

import com.orbitz.consul.Consul;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;

import java.util.*;

public class ConsulConnection {
    private Consul consulClient;
    private String serviceId;

    public ConsulConnection(String url, String id, String name, String ip) {
        serviceId = id;
        consulClient = Consul.builder().withUrl(url).build();

        Registration service = ImmutableRegistration.builder()
                .id(id)
                .name(name)
                .address(ip)
                .port(8000)
                .tags(Collections.singletonList(id))
                .meta(Collections.singletonMap("version", "1.0"))
                .build();

        consulClient.agentClient().register(service);
    }

    public void close() {
        consulClient.agentClient().deregister(serviceId);
    }


    public String getConfigValueAsString(String key) {
        String value = consulClient.keyValueClient().getValueAsString(key).get();
        System.out.println("Consul KV (string): " + key + " : " + value);
        return value;
    }

    public int getConfigValueAsInt(String key) {
        String sValue = consulClient.keyValueClient().getValueAsString(key).get();
        int value = Integer.parseInt(sValue);
        System.out.println("Consul KV (int): " + key + " : " + value);
        return value;
    }

    public List<String> getServices(String name) {
        ConsulResponse<List<CatalogService>> consulResponse = consulClient.catalogClient().getService(name);
        List<CatalogService> list = consulResponse.getResponse();
        System.out.println(name + " services - found " + list.size());
        List<String> result = new ArrayList<>();
        for (CatalogService catalogService : list) {
            result.add(catalogService.getServiceAddress() + ":" + catalogService.getServicePort());
        }
        return result;
    }


}
