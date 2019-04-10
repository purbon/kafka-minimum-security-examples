package com.purbon.kafka;

import java.util.HashMap;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;

public class App {

  public static Properties config() {

    final Properties config = new Properties();
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, "porsche-streams-app");
    config.put(StreamsConfig.CLIENT_ID_CONFIG, "porsche-streams-app-client1");

    config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
    config.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);

    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
    config.put("sasl.mechanism", "PLAIN");

    String saslJaasString = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"alice\" password=\"alice-secret\";";
    config.put("sasl.jaas.config", saslJaasString);

    return config;

  }

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
