# kconnect-samples

This repository contains sample code for using Kafka Connect with Confluent Cloud.
It shows how to use the Confluent Cloud CLI to create a Kafka cluster and create a connector.

You can find the following samples in this repository in the order :
- [check-integrity-transformer](check-integrity-transformer/readme.md) : This sample shows how to use the `transform` feature of Kafka Connect to add a new field to the record and check the integrity of the record.
- [simple-transformers](simple-transformers/readme.md) : This sample shows how to use the `transform` feature of Kafka Connect to add a new field to the record and other simple examples like rename field and identity.
- [json-transformer](json-transformer/readme.md) : This sample shows how to use the `transform` feature of Kafka Connect to manipulate JSON objects with kafka connect transformers.