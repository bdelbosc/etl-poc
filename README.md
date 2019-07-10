# ETL Poc

## About
This is proof of concept showing how to use [Nuxeo Stream](https://github.com/nuxeo/nuxeo/tree/master/nuxeo-runtime/nuxeo-stream) to extract content from an existing system,
and load it into Nuxeo ([ETL](https://en.wikipedia.org/wiki/Extract,_transform,_load)). 

## Setup

### Kafka

Kafka is needed to have a distributed processing with failover.
For testing just run it locally using docker-compose:

To start the Kafka server:
```bash
(cd docker/kafka; docker-compose up -d)
```  

To stop it:
```bash
(cd docker/kafka; docker-compose down --volume)
```

Data are persisted in a volume under `docker/kafka/data`.

The Kafka broker is exposed on `localhost:9092`.

### Monitoring

For the monitoring we need a Prometheus/Grafana stack, again use docker-compose to run it locally:

To start the Kafka server 
```bash
(cd docker/monitoring; docker-compose up -d)
```  

To stop it:
```bash
(cd docker/monitoring; docker-compose down --volume)
```

You should have a Grafana running on http://localhost:3000/ (admin/admin)
And Prometheus on http://localhost:9090/

## Extract

The first phase is to extract the content from an existing system,
because this task is very specific to the underlying system, the content here is generated on the fly,
so we can focus on the production of messages that represent the content.

A message represents a document and its properties using a JSON payload.

Extract is a standalone application that uses `nuxeo-stream` library to append messages to a Log storage.

The underlying Log can be:
 - a local directory when using Chronicle Queue
 - a distributed Log (a topic) when using Kafka

The log is partitioned to enable parallelism in the downstream processing.
The message is routed to a partition depending on its Key.

The extract is done with concurrency to improve the throughput.

### Extract configuration

Edit the code to change the concurrency and/or number of messages to produce. 

### Run extraction

Run the unit test to execute the extractor:

```bash
(cd extract; mvn -nsu clean test)
...
Using Chronicle Queue
1000 records appended to log: 'extract', using 10 threads in 163 ms, throughput: 6134.97 recs/s

Using Kafka
1000 records appended to log: 'extract', using 10 threads in 734 ms, throughput: 1362.40 recs/s

```  
## Transform

If needed messages can be transformed before being loaded.

TODO:
- Add a consumer/producer that enrich the message. 

## Load

The load phase is responsible to create Nuxeo documents from the extracted messages.

This is driven by a Nuxeo automation operation.
The operation can be invoked on multiple Nuxeo nodes to increase the throughput.

The maximum concurrency is defined by the number of partition in the input Log.

TODO:
- parse the payload and create a document in
  org.nuxeo.sample.etl.load.ContentMessageConsumer.accept

### Run load

With the unit test:
```bash
(cd load; mvn -nsu clean test)
...
Using Kafka
12:11:59,537 [main] WARN  [LoadOperation] Import documents from log: extract into: test//, with policy: DocumentConsumerPolicy{blockIndexing=false, bulkMode=false, blockAsyncListeners=false, blockPostCommitListeners=false, blockDefaultSyncListeners=false, ConsumerPolicy{batchPolicy=BatchPolicy{capacity=10, threshold=PT20S}, retryPolicy=net.jodah.failsafe.RetryPolicy@2cc03cd1, skipFailure=false, waitMessageTimeout=PT1S, startOffset=LAST_COMMITTED, salted=false, name='Load.run', maxThreads=4}}
12:11:59,942 [Nuxeo-ConsumerPool-00] WARN  [AbstractCallablePool] Start Nuxeo-Consumer Pool on 4 thread(s).
12:12:01,431 [Nuxeo-ConsumerPool-00] WARN  [ConsumerPool] Consumers status: threads: 4, failure 0, messages committed: 1000, elapsed: 1.45s, throughput: 692.04 msg/s
Using Chronicle Queue
12:12:01,501 [main] WARN  [LoadOperation] Import documents from log: extract into: test//, with policy: DocumentConsumerPolicy{blockIndexing=false, bulkMode=false, blockAsyncListeners=false, blockPostCommitListeners=false, blockDefaultSyncListeners=false, ConsumerPolicy{batchPolicy=BatchPolicy{capacity=10, threshold=PT20S}, retryPolicy=net.jodah.failsafe.RetryPolicy@6c9320c2, skipFailure=false, waitMessageTimeout=PT1S, startOffset=LAST_COMMITTED, salted=false, name='Load.run', maxThreads=4}}
12:12:01,661 [Nuxeo-ConsumerPool-00] WARN  [AbstractCallablePool] Start Nuxeo-Consumer Pool on 4 thread(s).
12:12:02,798 [Nuxeo-ConsumerPool-00] WARN  [ConsumerPool] Consumers status: threads: 4, failure 0, messages committed: 1000, elapsed: 1.11s, throughput: 900.09 msg/s

```

Using curl:
```bash
curl -X POST 'http://localhost:8080/nuxeo/site/automation/Load.run' -u Administrator:Administrator -H 'content-type: application/json' \
-d '{"params":{"rootFolder": "/default-domain/workspaces"}}'
```
For a list of available parameters visit the StreamImporter.runDocumentConsumers operation on the [nuxeo-importer-stream doc](https://github.com/nuxeo/nuxeo/tree/master/addons/nuxeo-platform-importer/nuxeo-importer-stream#two-steps-import-generate-and-import-documents-with-blobs).

## Monitoring

The Load operation exposes metrics per node via JMX (like other Nuxeo metrics).

This metrics are valuable to understand the load, they can be aggregated but will be incorrect if a Nuxeo node is restarted.

To get the correct metrics cluster wide the information need to be taken from Kafka,
a separate program need to be run in order to monitor the topic usage and expose the lag.

TODO:
- write stream.sh monitor equivalent that monitor the lag of a topic and expose it to prometheus 
- configure prometheus jmx-exporter to export Nuxeo metrics
- build a grafana dashboard 
 
## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.

