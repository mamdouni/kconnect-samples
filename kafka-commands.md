# Kafka Commands


## Handy commands to manipulate kafka objects

### Topics

Create a topic :

````shell
$ docker exec -it kafka kafka-topics --create --bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 3 \
 --topic my-first-topic
````

Describe topic :

````shell
$ docker exec -it kafka kafka-topics --describe --bootstrap-server kafka:9092 --topic my-first-topic
````

Describe all topic :

````shell
$ docker exec -it kafka kafka-topics --describe --bootstrap-server kafka:9092
````

List of topics :
````shell
$ docker exec -it kafka kafka-topics --list --bootstrap-server kafka:9092
````

Delete topic :
````shell
$ docker exec -it kafka kafka-topics --bootstrap-server kafka:9092 --delete --topic my-first-topic
````

### Producers

Create a producer :

````shell
$ docker exec -it kafka kafka-console-producer --broker-list localhost:9092 --topic my-first-topic
````

### Consumers

Create a consumer :

````shell
$ docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic my_topic
````

This will consume only new messages. You can read from the beginning by passing this option `--from-beginning`

### Schema Registry

Get all schemas :

````shell
$ curl http://localhost:8081/subjects
````

Get the schema versions :

````shell
$ curl http://localhost:8081/subjects/${TOPIC_NAME}-value/verions
````

Get a schema version details :

````shell
$ curl http://localhost:8081/subjects/${TOPIC_NAME}-value/verions/${ID}
````

Delete a schema version :

````shell
curl -X DELETE https://public-decathlon-aoaa-vcstream-dev-01-peered-aoaa.aivencloud.com:12661/subjects/persons.topic-value/versions/1
````

If you have only one version of the schema, the topic will be also deleted from the subject.

## References
- https://gist.github.com/geunho/77f3f9a112ea327457353aa407328771