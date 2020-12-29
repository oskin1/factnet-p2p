FROM openjdk:8-jre-slim as builder
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt
COPY . /factnet-p2p
WORKDIR /factnet-p2p
RUN sbt assembly
RUN mv `find . -name factnet-p2p-assembly-*.jar` /factnet.jar
CMD ["/usr/bin/java", "-jar", "/factnet.jar"]

FROM openjdk:8-jre-slim
COPY --from=builder /factnet.jar /factnet.jar
ENTRYPOINT ["java", "-jar", "/factnet.jar"]