import brave.Tracing;
import brave.opentracing.BraveTracer;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
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
import javax.ws.rs.core.MultivaluedMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Publisher extends Application<Configuration> {

    private final Tracer tracer;

    public Publisher(Tracer tracer){
        this.tracer = tracer;
    }

    @Path("/publish")
    @Produces(MediaType.TEXT_PLAIN)
    public class PublisherResource{

        @GET
        public String format(@QueryParam("helloStr") String helloStr, @Context HttpHeaders httpHeaders){
            SpanContext spanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                    new TextMapExtractAdapter(convertMultiToRegularMap(httpHeaders.getRequestHeaders())));

            StringBuilder sb = new StringBuilder();
            sb.append("/publish?helloStr=");
            sb.append(helloStr);
            String helloStrTag = sb.toString();

            Span span = tracer.buildSpan("publishSpan")
                    .asChildOf(spanContext)
                    .withTag("component","dropwizard")
                    .withTag("http.method", "GET")
                    .withTag("http.url",helloStrTag)
                    .withTag("span.kind","server")
                    .start();

            System.out.println(helloStr);
            span.log(ImmutableMap.of("event","println","value",helloStr));

            span.finish();
            return "published";
        }

        private Map<String, String> convertMultiToRegularMap(MultivaluedMap<String, String> m) {
            Map<String, String> map = new HashMap<>();
            if (m == null) {
                return map;
            }
            for (Map.Entry<String, List<String>> entry : m.entrySet()) {
                StringBuilder sb = new StringBuilder();
                for (String s : entry.getValue()) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(s);
                }
                map.put(entry.getKey(), sb.toString());
            }
            return map;
        }

    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception{
        environment.jersey().register(new PublisherResource());
    }

    public static void main(String[] args) throws Exception {
        //Zipkin configuration, using zipkin-sender-okhttp3 and brave-opentracing dependencies
        Sender sender = OkHttpSender.create("http://zipkin:9411/api/v2/spans");
        Reporter spanReporter = AsyncReporter.create(sender);
        Tracing braveTracing = Tracing.newBuilder().localServiceName("publisher").spanReporter(spanReporter).build();
        Tracer tracer = BraveTracer.create(braveTracing);

        //Init publisher server.
        new Publisher(tracer).run(args);
    }

}
