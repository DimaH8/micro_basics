#!/bin/bash

set -xe

mvn clean package
sudo docker build -f DockerfileFacadeService -t facade-service:1.0 .
sudo docker build -f DockerfileLoggingService -t logging-service:1.0 .
sudo docker build -f DockerfileMessagesService -t messages-service:1.0 .

