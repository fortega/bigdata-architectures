# BigData architectures in Scala

## About

This is a project to learn different data processing architectures using Scala and Domain-driven design.

Problem: your GPS provider somethings send you bad data. You have to validate each event, and provide information about your findings

## How to run

### Batch (Spark)

Using "input" for input files (parquet) and "output" to write validated events.

> sbt "batch/run input output"

### Lambda (Flink)

> sbt lambda/run

## Application structure

* core: bussiness logic
* batch: batch processing in Apache Spark
* lambda: stream processing in Apache Flink
* beam: stream or batch processing in Apache beam (scio)

## Theory

### Classic "big data" or Batch architecture

https://en.wikipedia.org/wiki/Batch_processing

### Lambda architecture

https://en.wikipedia.org/wiki/Lambda_architecture

### Kappa architecture

https://en.wikipedia.org/wiki/Lambda_architecture#Kappa_architecture

### Beam model or "unified architecture"

https://www.oreilly.com/radar/the-world-beyond-batch-streaming-101/

### Domain-driven design

https://en.wikipedia.org/wiki/Domain-driven_design

