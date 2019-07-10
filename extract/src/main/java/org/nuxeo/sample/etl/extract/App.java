package org.nuxeo.sample.etl.extract;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.log.kafka.KafkaLogManager;
import org.nuxeo.sample.etl.ContentMessage;

/**
 * Extract content into a stream.
 */
public class App {

    protected static final Path CHRONICLE_PATH = Paths.get(System.getProperty("java.io.tmpdir"), "etl");

    protected static final String KAFKA_BOOTSTRAP_SERVER = "localhost:9092";

    protected static final String LOG_NAME = "extract";

    protected static final int LOG_SIZE = 4;

    protected static final int CONCURRENCY = 10;

    protected static final int NB_RECORD_PER_THREAD = 100;

    protected static final int TOTAL_RECORDS = CONCURRENCY * NB_RECORD_PER_THREAD;

    protected static final int DELAY_MS = 0;

    protected LogManager manager;

    protected LogAppender<ContentMessage> appender;

    public static void main(String[] args) {
        new App().run(false);
    }

    protected void run(boolean useKafka) {
        // init the log manager
        if (useKafka) {
            initKafkaLogManager();
        } else {
            initChronicleLogManager();
        }
        // Init a log if it does not already exists
        initLog();
        // Get an appender
        initAppender();
        // Run a thread pool of writer
        runConcurrentGenerator();
    }

    protected void runConcurrentGenerator() {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        Runnable writer = () -> {
            for (int i = 0; i < NB_RECORD_PER_THREAD; i++) {
                ContentMessage record = ContentMessage.random();
                // an appender is thread safe it can be shared between threads
                appender.append(record.getKey(), record);
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        long start = System.currentTimeMillis();
        for (int i = 0; i < CONCURRENCY; i++) {
            executor.submit(writer);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("ERROR Timeout waiting on executor");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        System.out.println(
                String.format("%d records appended to log: '%s', using %d threads in %s ms, throughput: %.2f recs/s",
                        TOTAL_RECORDS, LOG_NAME, CONCURRENCY, elapsed, TOTAL_RECORDS / (elapsed / 1000.0)));
    }

    protected void initChronicleLogManager() {
        System.out.println("Using Chronicle Queue");
        manager = new ChronicleLogManager(CHRONICLE_PATH);
    }

    protected void initKafkaLogManager() {
        System.out.println("Using Kafka");
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVER);
        Properties consumerProperties = producerProperties;
        manager = new KafkaLogManager("etl", producerProperties, consumerProperties);
    }

    protected void initLog() {
        manager.createIfNotExists(LOG_NAME, LOG_SIZE);
    }

    protected void initAppender() {
        appender = manager.getAppender(LOG_NAME);
    }
}
