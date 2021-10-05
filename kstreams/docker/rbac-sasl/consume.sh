#!/usr/bin/env bash

docker-compose exec broker kafka-console-consumer --bootstrap-server broker:9092  \
              --topic target-topic --from-beginning \
              --consumer.config /etc/client-configs/professor.properties
