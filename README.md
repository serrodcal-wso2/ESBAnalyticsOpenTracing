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

## Regarding custom observer

_Under construction_.

# Regarding OpenTracing

Enter OpenTracing: by offering consistent, expressive, vendor-neutral APIs for popular platforms, OpenTracing makes it easy for developers to add (or switch) tracing implementations with an O(1) configuration change. OpenTracing also offers a lingua franca for OSS instrumentation and platform-specific tracing helper libraries.

At the highest level, a trace tells the story of a transaction or workflow as it propagates through a (potentially distributed) system. In OpenTracing, a trace is a directed acyclic graph (DAG) of "spans": named, timed operations representing a contiguous segment of work in that trace.

![DAG example]http://opentracing.io/documentation/images/OTHT_1.png)

Each component in a distributed trace will contribute its own span or spans.

![](http://opentracing.io/documentation/images/OTOV_2.png)

Tracing a workflow or transaction through a distributed system often looks something like the above. While this type of visualization can be useful to see how various components fit together, it does not convey any time durations, does not scale well, and is cumbersome when parallelism is involved. Another limitation is that there is no way to easily show latency or other aspects of timing. A more useful way to visualize even a basic trace often looks like this:

![](http://opentracing.io/documentation/images/OTOV_3.png)

Using HTTP, each component propagates four headers as given below:

```
X-B3-TraceId
X-B3-ParentSpanId
X-B3-SpanId
X-B3-Sampled
```
