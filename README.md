# ESBAnalyticsOpenTracing

This repository contains the Docker build definition and release process for
a proof of concept about ESB Customizing Statics Publishing following OpenTracing.

## Regarding usage

There are six containers:

* MySQL (needed by Zipkin).
* Zipkin.
* WSO2ESB-5.0.0.
* Publisher application (_Microservice_).
* Formatter application (_Microservice_).
* Hello application (_Microservice_).

Hello is a main application which is responsible for the orchestration between
services, in this case two microservices: Formatter and Publisher. Formatter is a Java application
which receives an HTTP request with a name and returns _Hi, name_.
Publisher is a Java application which receives an HTTP request with a name and
says hello.

These modules, described above, send spans to a zipkin instance. ESB contains a
Carbon Application exposing the required resources by microservices and sends
statics to Zipkin as well.

**Note**: Hello, Formatter and Publisher application must be build running
`mvn clean package` command in theirs folders before running in docker.

## Running

For that, only run `docker-compose up` in root of this repository where
it is the docker-compose.yml file.

## Testing

For that, only run `$ curl 'http://localhost:8280/hello?helloTo=Sergio&greeting=Hola'`.

After that, access to Zipkin in http://localhost:9411 to see spans accross this small platform.

To test this services individually, run following commands:

```
$ curl 'http://localhost:8082/publish?helloStr=hi%20there'

$ curl 'http://localhost:8081/format?helloTo=Sergio'

$ curl 'http://localhost:8080/hello?helloTo=Sergio&greeting=Hola'
```
