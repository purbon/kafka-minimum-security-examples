package com.purbon.kafka;

import java.util.Properties;
import java.util.concurrent.Future;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.codehaus.jackson.map.ObjectMapper;

public class SimplyProducer {

  private ObjectMapper mapper = new ObjectMapper();
  private KafkaProducer<Long, String> producer = new KafkaProducer<>(configure());

  private Properties configure() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "my-producer");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer");
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
    props.put("sasl.mechanism", "PLAIN");
    String saslJaasString = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"alice\" password=\"alice-secret\";";
    props.put("sasl.jaas.config", saslJaasString);

    props.put(
        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor");

    props.put("confluent.monitoring.interceptor.sasl.mechanism", "PLAIN");
    props.put("confluent.monitoring.interceptor.security.protocol", "SASL_PLAINTEXT");
    props.put("confluent.monitoring.interceptor.sasl.jaas.config", saslJaasString);

    // To enable a shorter loop example we need to publish messages for monitoring more
    // often than the default values
    props.put("confluent.monitoring.interceptor.publishMs", "1000");
    return props;
  }

  public void send(String topic, long key, String value) {
    ProducerRecord<Long, String> record = new ProducerRecord<>(topic, key, value);
    producer.send(record);
    producer.flush();
  }

  public static void main(String[] args) throws Exception {

    SimplyProducer producer = new SimplyProducer();

    System.out.println("Sending a message to the topics");
    long now = System.currentTimeMillis();
    for(long i=now; i < now+5; i++) {
      producer.send("topicA", i, "message_with_value_" + i);
      Thread.sleep(1000);
    }
    System.out.println("done");


  }

}
