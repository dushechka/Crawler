FROM maven:3.5-jdk-8-alpine
ADD . /app
WORKDIR /app
RUN mvn clean package appassembler:assemble

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=0 /app /app
ENV PATH "$PATH:/app/target/appassembler/bin"
RUN echo $PATH
CMD ["crawler"]
