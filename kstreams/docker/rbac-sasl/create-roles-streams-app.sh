#!/usr/bin/env bash

## Login into MDS
XX_CONFLUENT_USERNAME=professor XX_CONFLUENT_PASSWORD=professor confluent login --url http://localhost:8090


ZK_CONTAINER=zookeeper
ZK_PORT=2181
echo "Retrieving Kafka cluster id from docker-container '$ZK_CONTAINER' port '$ZK_PORT'"
KAFKA_CLUSTER_ID=$(docker exec -it $ZK_CONTAINER zookeeper-shell localhost:$ZK_PORT get /cluster/id 2> /dev/null | grep \"version\" | jq -r .id)
if [ -z "$KAFKA_CLUSTER_ID" ]; then
    echo "Failed to retrieve kafka cluster id from zookeeper"
    exit 1
fi

## Create Service Roles
STREAMS_PRINCIPAL="User:zoidberg"

################################### STREAMS ###################################

echo "Creating Kafka Streams role bindings"

confluent iam rolebinding create \
    --principal $STREAMS_PRINCIPAL \
    --role DeveloperRead \
    --kafka-cluster-id $KAFKA_CLUSTER_ID \
    --resource "Topic:source-topic"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --resource "Topic:target-topic"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperRead \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperManage \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Topic:porsche-streams-app"


confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperRead \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperWrite \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"

confluent iam rolebinding create \
        --principal $STREAMS_PRINCIPAL \
        --role DeveloperManage \
        --kafka-cluster-id $KAFKA_CLUSTER_ID \
        --prefix \
        --resource "Group:porsche-streams-app"


confluent iam rolebinding list --principal $STREAMS_PRINCIPAL --kafka-cluster-id $KAFKA_CLUSTER_ID

docker-compose exec broker kafka-topics --bootstrap-server broker:9092  --create --topic source-topic --command-config /etc/client-configs/professor.properties
docker-compose exec broker kafka-topics --bootstrap-server broker:9092  --create --topic target-topic --command-config /etc/client-configs/professor.properties

## created roles
#Role       | ResourceType |        Name         | PatternType
#+-----------------+--------------+---------------------+-------------+
#DeveloperManage | Topic        | porsche-streams-app | PREFIXED
#DeveloperManage | Group        | porsche-streams-app | PREFIXED
#DeveloperRead   | Topic        | source-topic        | LITERAL
#DeveloperRead   | Topic        | porsche-streams-app | PREFIXED
#DeveloperRead   | Group        | porsche-streams-app | PREFIXED
#DeveloperWrite  | Topic        | target-topic        | LITERAL
#DeveloperWrite  | Topic        | porsche-streams-app | PREFIXED
#DeveloperWrite  | Group        | porsche-streams-app | PREFIXED
