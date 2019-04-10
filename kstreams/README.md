# Kafka Streams minimum security prison

One of the main objective of a secured environment, but not just on that, as well for multi tenant deployments is to allow each app to run with the minimum permissions possible.

In Apache Kafka this would mean having controlling the access using ACL's, the minimum for a Kafka Streams App are:

```
# Allow Streams to read the input topics:
kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Read --topic source-topic

# Allow Streams to write to the output topics:
kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation Write --topic target-topic

# Allow Streams to manage its own internal topics and consumer groups:
kafka-acls -authorizer-properties zookeeper.connect=zookeeper:2181 --add --allow-principal User:alice --operation All --resource-pattern-type prefixed --topic porsche-streams-app --group porsche-streams-app
```

That basically mean allowing the user principal to read from any source topics, write to target topics and perform any sort of operations to internal topics such as the ones created prefixed with the app name.

This example contains two main components to test this out.

## An Apache Kafka docker cluster

This would be our base to operate this example. You can start it by running the _./up_ script, this shell script would be responsible of
pulling in all docker images, starting them, create the topics and ACL's necessary to run.

You should see something like this to confirm everything is up and running:

```
➜  kstreams docker ps -a
CONTAINER ID        IMAGE                                    COMMAND                  CREATED              STATUS              PORTS                                        NAMES
43396e1ba299        confluentinc/cp-enterprise-kafka:5.1.0   "/etc/confluent/dock…"   About a minute ago   Up About a minute   0.0.0.0:9092->9092/tcp                       kstreams_kafka_1
989266aaae36        confluentinc/cp-zookeeper:5.1.0          "/etc/confluent/dock…"   About a minute ago   Up About a minute   2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp   kstreams_zookeeper_1
```

This example contains two users, Alice and Bob that will be useful to run the different operations. While Alice has permissions to run anything she need, as well is the principal for the Kafka Streams app, Bob is not authorised to do anything.

A super user _User:kafka_ is created.

```
Example commands:
Should succeed (alice authorized)
-> docker-compose exec kafka kafka-console-producer --broker-list kafka:9092 --topic target-topic --producer.config=/etc/kafka/alice.properties
Should fail (bob is NOT authorized)
-> docker-compose exec kafka kafka-console-producer --broker-list kafka:9092 --topic target-topic --producer.config=/etc/kafka/bob.properties
```

This example is done using SASL plain for simplicity, however all the principles are applicable using other authorisation methods such as Kerberos, etc. All we need are the minimum ACL's.

## A simple Kafka streams app

As well inside the directory [app](app/) you'll find a simple KStreams app. This app will read from a topic and copy it's content to another topic, but in upper case.


```java
public class App {

  public static void main(String[] args) {

    StreamsBuilder builder = new StreamsBuilder();

    builder
        .stream("source-topic",
            Consumed.with(Serdes.String(), Serdes.String()))
        .mapValues(s -> s.toUpperCase())
        .to("target-topic", Produced.with(Serdes.String(), Serdes.String()));


    final KafkaStreams streams = new KafkaStreams(builder.build(), config());
    streams.cleanUp();
    streams.start();

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

  }
}
```

This project is configured to authenticate with the a cluster using SASL plain.

```java
config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
config.put("sasl.mechanism", "PLAIN");

String saslJaasString = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"alice\" password=\"alice-secret\";";
config.put("sasl.jaas.config", saslJaasString);
```

this can be adapted to other methods if need.


## Running the example

To execute this example you will need to:

* Start the dockerised Apache Kafka Cluster using the _./up_ script.
* Execute the streams app located inside [app](app/)

After doing that, every message you write inside _source-topic_ , you will see how the content will get copied to the _target-topic_ but in upper case. To do that remember the commands we shared earlier in this readme.

```
Example commands:
Should succeed (alice authorized)
-> docker-compose exec kafka kafka-console-producer --broker-list kafka:9092 --topic target-topic --producer.config=/etc/kafka/alice.properties
Should fail (bob is NOT authorized)
-> docker-compose exec kafka kafka-console-producer --broker-list kafka:9092 --topic target-topic --producer.config=/etc/kafka/bob.properties
```

## Contributing

All contributions are welcome: ideas, patches, documentation, bug reports,
complaints, etc!

Programming is not a required skill, and there are many ways to help out!
