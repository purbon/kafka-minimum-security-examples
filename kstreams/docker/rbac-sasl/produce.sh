#!/usr/bin/env bash

docker-compose exec broker kafka-console-producer --broker-list broker:9092 \
                    --topic source-topic \
                     --property "parse.key=true" --property "key.separator=:" \
                     --producer.config /etc/client-configs/professor.properties
