#!/bin/bash

set -ex

sudo docker exec consul-server consul kv put app/config/facade-service/port "8001"
sudo docker exec consul-server consul kv put app/config/logging-service/port "8002"
sudo docker exec consul-server consul kv put app/config/messages-service/port "8003"
sudo docker exec consul-server consul kv put app/config/all-hazelcast-instances "192.168.0.11:5701,192.168.0.12:5701,192.168.0.13:5701"
sudo docker exec consul-server consul kv put app/config/all-logging-services "192.168.0.112:8002,192.168.0.122:8002,192.168.0.132:8002"
sudo docker exec consul-server consul kv put app/config/all-messages-services "192.168.0.113:8003,192.168.0.123:8003"
sudo docker exec consul-server consul kv put app/config/hazelcast/map/name "loggingServiceMap"
sudo docker exec consul-server consul kv put app/config/hazelcast/queue/name "facade2messagesQueue"
