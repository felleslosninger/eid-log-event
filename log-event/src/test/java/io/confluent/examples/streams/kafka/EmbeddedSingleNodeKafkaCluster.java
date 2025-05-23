/*
 * Copyright Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.examples.streams.kafka;

import io.confluent.examples.streams.zookeeper.ZooKeeperEmbedded;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.RestApp;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig;
import org.apache.kafka.network.SocketServerConfigs;
import org.apache.kafka.server.config.ServerConfigs;
import org.apache.kafka.server.config.ServerLogConfigs;
import org.apache.kafka.storage.internals.log.CleanerConfig;
import org.apache.kafka.test.TestCondition;
import org.apache.kafka.test.TestUtils;
import org.apache.kafka.server.config.ZkConfigs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.CollectionConverters;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * From https://github.com/confluentinc/kafka-streams-examples/
 * Runs an in-memory, "embedded" Kafka cluster with 1 ZooKeeper instance, 1 Kafka broker, and 1
 * Confluent Schema Registry instance.
 */
public class EmbeddedSingleNodeKafkaCluster {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSingleNodeKafkaCluster.class);
    private static final int DEFAULT_BROKER_PORT = 1234; // pick a random port
    private static final String KAFKA_SCHEMAS_TOPIC = "_schemas";
    private static final String AVRO_COMPATIBILITY_TYPE = CompatibilityLevel.NONE.name;

    private static final String KAFKASTORE_OPERATION_TIMEOUT_MS = "60000";
    private static final String KAFKASTORE_DEBUG = "true";
    private static final String KAFKASTORE_INIT_TIMEOUT = "90000";
    private final Properties brokerConfig;
    private ZooKeeperEmbedded zookeeper;
    private KafkaEmbedded broker;
    private RestApp schemaRegistry;
    private boolean running;

    /**
     * Creates and starts the cluster.
     */
    public EmbeddedSingleNodeKafkaCluster() {
        this(new Properties());
    }

    /**
     * Creates and starts the cluster.
     *
     * @param brokerConfig Additional broker configuration settings.
     */
    public EmbeddedSingleNodeKafkaCluster(final Properties brokerConfig) {
        this.brokerConfig = new Properties();
        this.brokerConfig.put(SchemaRegistryConfig.KAFKASTORE_TIMEOUT_CONFIG, KAFKASTORE_OPERATION_TIMEOUT_MS);
        this.brokerConfig.putAll(brokerConfig);
    }

    /**
     * Creates and starts the cluster.
     */
    public void start() throws Exception {
        log.debug("Initiating embedded Kafka cluster startup");
        log.debug("Starting a ZooKeeper instance...");
        zookeeper = new ZooKeeperEmbedded();
        log.debug("ZooKeeper instance is running at {}", zookeeper.connectString());

        final Properties effectiveBrokerConfig = effectiveBrokerConfigFrom(brokerConfig, zookeeper);
        log.debug("Starting a Kafka instance on port {} ...",
                effectiveBrokerConfig.getProperty(SocketServerConfigs.LISTENERS_CONFIG));
        broker = new KafkaEmbedded(effectiveBrokerConfig);
        log.debug("Kafka instance is running at {}, connected to ZooKeeper at {}",
                broker.brokerList(), broker.zookeeperConnect());

        final Properties schemaRegistryProps = new Properties();

        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_TIMEOUT_CONFIG, KAFKASTORE_OPERATION_TIMEOUT_MS);
        schemaRegistryProps.put(SchemaRegistryConfig.DEBUG_CONFIG, KAFKASTORE_DEBUG);
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_INIT_TIMEOUT_CONFIG, KAFKASTORE_INIT_TIMEOUT);
        schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG, effectiveBrokerConfig.getProperty(SocketServerConfigs.LISTENERS_CONFIG));

        schemaRegistry = new RestApp(0, null, KAFKA_SCHEMAS_TOPIC, AVRO_COMPATIBILITY_TYPE, schemaRegistryProps);
        schemaRegistry.start();
        running = true;
    }

    private Properties effectiveBrokerConfigFrom(final Properties brokerConfig, final ZooKeeperEmbedded zookeeper) {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.putAll(brokerConfig);
        effectiveConfig.put(ZkConfigs.ZK_CONNECT_CONFIG, zookeeper.connectString());
        effectiveConfig.put(ZkConfigs.ZK_SESSION_TIMEOUT_MS_CONFIG, 30 * 1000);
        effectiveConfig.put(SocketServerConfigs.LISTENERS_CONFIG, String.format("PLAINTEXT://127.0.0.1:%s", DEFAULT_BROKER_PORT));
        effectiveConfig.put(ZkConfigs.ZK_CONNECTION_TIMEOUT_MS_CONFIG, 60 * 1000);
        effectiveConfig.put(ServerConfigs.DELETE_TOPIC_ENABLE_CONFIG, true);
        effectiveConfig.put(CleanerConfig.LOG_CLEANER_DEDUPE_BUFFER_SIZE_PROP, 2 * 1024 * 1024L);
        effectiveConfig.put(GroupCoordinatorConfig.GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, 0);
        effectiveConfig.put(GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, (short) 1);
        effectiveConfig.put(GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, 1);
        effectiveConfig.put(ServerLogConfigs.AUTO_CREATE_TOPICS_ENABLE_CONFIG, true);
        return effectiveConfig;
    }

  @BeforeEach
  protected void before() throws Exception {
    start();
  }

  @AfterEach
  protected void after() {
    stop();
  }

    /**
     * Stops the cluster.
     */
    public void stop() {
        log.info("Stopping Confluent");
        try {
            try {
                if (schemaRegistry != null) {
                    schemaRegistry.stop();
                }
            } catch (final Exception fatal) {
                throw new RuntimeException(fatal);
            }
            if (broker != null) {
                broker.stop();
            }
            try {
                if (zookeeper != null) {
                    zookeeper.stop();
                }
            } catch (final IOException fatal) {
                throw new RuntimeException(fatal);
            }
        } finally {
            running = false;
        }
        log.info("Confluent Stopped");
    }

    /**
     * This cluster's `bootstrap.servers` value.  Example: `127.0.0.1:9092`.
     * <p>
     * You can use this to tell Kafka Streams applications, Kafka producers, and Kafka consumers (new
     * consumer API) how to connect to this cluster.
     */
    public String bootstrapServers() {
        return broker.brokerList();
    }

    /**
     * The "schema.registry.url" setting of the schema registry instance.
     */
    public String schemaRegistryUrl() {
        return schemaRegistry.restConnect;
    }

    /**
     * Creates a Kafka topic with 1 partition and a replication factor of 1.
     *
     * @param topic The name of the topic.
     */
    public void createTopic(final String topic) throws InterruptedException {
        createTopic(topic, 1, (short) 1, Collections.emptyMap());
    }

    /**
     * Creates a Kafka topic with the given parameters.
     *
     * @param topic       The name of the topic.
     * @param partitions  The number of partitions for this topic.
     * @param replication The replication factor for (the partitions of) this topic.
     */
    public void createTopic(
            final String topic,
            final int partitions,
            final short replication) throws InterruptedException {
        createTopic(topic, partitions, replication, Collections.emptyMap());
    }

    /**
     * Creates a Kafka topic with the given parameters.
     *
     * @param topic       The name of the topic.
     * @param partitions  The number of partitions for this topic.
     * @param replication The replication factor for (partitions of) this topic.
     * @param topicConfig Additional topic-level configuration settings.
     */
    public void createTopic(
            final String topic,
            final int partitions,
            final short replication,
            final Map<String, String> topicConfig) throws InterruptedException {
        createTopic(60000L, topic, partitions, replication, topicConfig);
    }

    /**
     * Creates a Kafka topic with the given parameters and blocks until all topics got created.
     *
     * @param topic       The name of the topic.
     * @param partitions  The number of partitions for this topic.
     * @param replication The replication factor for (partitions of) this topic.
     * @param topicConfig Additional topic-level configuration settings.
     */
    public void createTopic(
            final long timeoutMs,
            final String topic,
            final int partitions,
            final short replication,
            final Map<String, String> topicConfig) throws InterruptedException {
        broker.createTopic(topic, partitions, replication, topicConfig);

        if (timeoutMs > 0) {
            TestUtils.waitForCondition(new TopicCreatedCondition(topic), timeoutMs, "Topics not created after " + timeoutMs + " milli seconds.");
        }
    }

    /**
     * Deletes multiple topics and blocks until all topics got deleted.
     *
     * @param timeoutMs the max time to wait for the topics to be deleted (does not block if {@code <= 0})
     * @param topics    the name of the topics
     */
    public void deleteTopicsAndWait(final long timeoutMs, final String... topics) throws InterruptedException {
        for (final String topic : topics) {
            try {
                broker.deleteTopic(topic);
            } catch (final UnknownTopicOrPartitionException expected) {
                // indicates (idempotent) success
            }
        }

        if (timeoutMs > 0) {
            TestUtils.waitForCondition(new TopicsDeletedCondition(topics), timeoutMs, "Topics not deleted after " + timeoutMs + " milli seconds.");
        }
    }

    public boolean isRunning() {
        return running;
    }

    private final class TopicsDeletedCondition implements TestCondition {
        final Set<String> deletedTopics = new HashSet<>();

        private TopicsDeletedCondition(final String... topics) {
            Collections.addAll(deletedTopics, topics);
        }

        @Override
        public boolean conditionMet() {
            //TODO once KAFKA-6098 is fixed use AdminClient to verify topics have been deleted
            // The Set returned from JavaConverters.setAsJavaSetConverter does not support the remove
            // method so we need to continue to wrap in a HashSet
            final Set<String> allTopicsFromZk = new HashSet<>(
                    CollectionConverters.SetHasAsJava(broker.kafkaServer().zkClient().getAllTopicsInCluster(false))
                            .asJava());

            final Set<String> allTopicsFromBrokerCache = new HashSet<>(
                    CollectionConverters.SeqHasAsJava(broker.kafkaServer()
                            .metadataCache()
                            .getAllTopics()
                            .toSeq()).asJava());

            return !allTopicsFromZk.removeAll(deletedTopics) && !allTopicsFromBrokerCache.removeAll(deletedTopics);
        }
    }

    private final class TopicCreatedCondition implements TestCondition {
        final String createdTopic;

        private TopicCreatedCondition(final String topic) {
            createdTopic = topic;
        }

        @Override
        public boolean conditionMet() {
            //TODO once KAFKA-6098 is fixed use AdminClient to verify topics have been deleted
            return broker.kafkaServer().zkClient().getAllTopicsInCluster(false).contains(createdTopic) &&
                    broker.kafkaServer().metadataCache().contains(createdTopic);
        }
    }

}
