FROM airdock/oracle-jdk:jdk-1.8

RUN apt-get update && \
    apt-get install -y zip wget vim && \
    wget -P /opt https://s3-us-west-2.amazonaws.com/wso2-stratos/wso2esb-5.0.0.zip && \
    apt-get clean && \
    unzip /opt/wso2esb-5.0.0.zip -d /opt && \
    rm /opt/wso2esb-5.0.0.zip

RUN rm /opt/wso2esb-5.0.0/repository/conf/carbon.xml && \
    rm /opt/wso2esb-5.0.0/repository/conf/synapse.properties && \
    rm /opt/wso2esb-5.0.0/bin/wso2server.sh

COPY ./bin/wso2server.sh /opt/wso2esb-5.0.0/bin/wso2server.sh
COPY ./observer/org.serrodcal.custom.publisher.zipkin-opentracing_1.0.jar /opt/wso2esb-5.0.0/repository/components/dropins/org.serrodcal.custom.publisher.zipkin-opentracing_1.0.jar
COPY ./carbon/carbon.xml /opt/wso2esb-5.0.0/repository/conf/carbon.xml
COPY ./synapse/synapse.properties /opt/wso2esb-5.0.0/repository/conf/synapse.properties

EXPOSE 9443 8243 8280 19444
WORKDIR /opt/wso2esb-5.0.0
CMD ["bin/wso2server.sh"]
