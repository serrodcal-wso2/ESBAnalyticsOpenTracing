import brave.Tracing;
import brave.opentracing.BraveTracer;
import ch.qos.logback.core.net.SyslogOutputStream;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Hello extends Application<Configuration> {

    private final Tracing tracing;
    private final OkHttpClient client;

    public Hello(){
        Sender sender = OkHttpSender.create("http://zipkin:9411/api/v2/spans");
        Reporter spanReporter = AsyncReporter.create(sender);
        Tracing braveTracing = Tracing.newBuilder().localServiceName("hello").spanReporter(spanReporter).build();
        this.tracing = braveTracing;
        this.client = new OkHttpClient();
    }

    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public class HelloResource{

        private Tracing tracing;

        public HelloResource(Tracing tracing) {
            this.tracing = tracing;
        }

        private class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {

            private final Request.Builder builder;

            RequestBuilderCarrier(Request.Builder builder) {
              this.builder = builder;
            }

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("carrier is write-only");
            }

            @Override
            public void put(String key, String value) {
              builder.addHeader(key, value);
            }

        }

        private String getHttp(Tracer tracer, Span spanRoot, String host, int port, String path, String param, String value) {
            Span span = tracer.buildSpan("get-http")
                  .asChildOf(spanRoot)
                  .withTag("component","dropwizard")
                  .withTag("peer.address", host)
                  .withTag("peer.port", port)
                  .withTag("peer.service", host)
                  .withTag("span.kind","client")
                  .start();
            try {
                HttpUrl url = new HttpUrl.Builder().scheme("http").host(host).port(port).addPathSegment(path).addQueryParameter(param,value).build();
                Request.Builder requestBuilder = new Request.Builder().url(url);

                Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
                Tags.HTTP_METHOD.set(span, "GET");
                Tags.HTTP_URL.set(span, url.toString());
                tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));

                span.log(ImmutableMap.of("event","println","status","Request!"));

                Request request = requestBuilder.build();
                Response response = client.newCall(request).execute();
                if (response.code() != 200) {
                throw new RuntimeException("Bad HTTP result: " + response);
                }
                span.finish();
                return response.body().string();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
        }

      private void sayHello(Tracer tracer, Span spanRoot, String helloTo, String greeting){
          String helloStr = formatString(tracer, spanRoot, helloTo);
          printHello(tracer, spanRoot, helloStr);
      }

      private String formatString(Tracer tracer, Span spanRoot, String helloTo) {
          String helloStr = getHttp(tracer, spanRoot, "formatter",8081, "format", "helloTo", helloTo);
          return helloStr;
      }

      private void printHello(Tracer tracer, Span spanRoot, String helloStr) {
          getHttp(tracer, spanRoot, "publisher", 8082, "publish", "helloStr", helloStr);
      }

      @GET
      public String format(@QueryParam("helloTo") String helloTo, @QueryParam("greeting") String greeting, @Context HttpHeaders httpHeaders){
          Tracer tracer = BraveTracer.create(this.tracing);

          StringBuilder sb = new StringBuilder();
          sb.append("/hello?helloTo=");
          sb.append(helloTo);
          sb.append("&greeting=");
          sb.append(greeting);
          String helloToTag = sb.toString();

          Span span = tracer.buildSpan("hello orchestator")
                  .withTag("component","dropwizard")
                  .withTag("http.method", "GET")
                  .withTag("http.url",helloToTag)
                  .start();

          System.out.println(helloTo);
          span.log(ImmutableMap.of("event","println","value",helloTo));

          System.out.println(greeting);
          span.log(ImmutableMap.of("event","println","value",greeting));

          this.sayHello(tracer, span, helloTo, greeting);

          span.finish();

          return "hello published";
      }

    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception{
        environment.jersey().register(new HelloResource(this.tracing));
    }

      public static void main(String[] args) throws Exception {

          new Hello().run(args);

      }

}
