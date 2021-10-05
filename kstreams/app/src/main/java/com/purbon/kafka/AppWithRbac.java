package com.purbon.kafka;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Reducer;

public class AppWithRbac {

  public static Properties config() {

    final Properties config = new Properties();
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, "porsche-streams-app");
    config.put(StreamsConfig.CLIENT_ID_CONFIG, "porsche-streams-app-client1");

    config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);
    config.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);

    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
    config.put("sasl.mechanism", "OAUTHBEARER");

    String saslJaasString = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required username=\"zoidberg\" password=\"zoidberg\" metadataServerUrls=\"http://localhost:8090\";";
    config.put("sasl.jaas.config", saslJaasString);

    config.put("sasl.login.callback.handler.class", "io.confluent.kafka.clients.plugins.auth.token.TokenUserLoginCallbackHandler");

    return config;


  }

  public static void main(String[] args) {

    AtomicLong counter = new AtomicLong(1);

    StreamsBuilder builder = new StreamsBuilder();

    builder
        .stream("source-topic",
            Consumed.with(Serdes.String(), Serdes.String()))
            .peek((k, v) -> System.out.println(k + " - " + v))
            .mapValues(s -> s + " - " + counter.incrementAndGet())
            .peek((k, v) -> System.out.println(k + " - " + v))
            .groupByKey()
            .reduce(new Reducer<String>() {
              @Override
              public String apply(String aggValue, String newValue) {
                String[] fields = aggValue.split("-");
                int aggCounter = Integer.valueOf(fields[1].trim());
                int newCounter = Integer.valueOf(newValue.split("-")[1].trim());
                return fields[0] + " - " + (newCounter+aggCounter);
              }
            }, Materialized.as("foo"))
            .toStream()
            .to("target-topic", Produced.with(Serdes.String(), Serdes.String()));


    final KafkaStreams streams = new KafkaStreams(builder.build(), config());

    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        System.out.println(e);
      }
    });

    streams.cleanUp();
    streams.start();

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

  }

}
