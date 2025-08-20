FROM openjdk:17
EXPOSE 8081
ADD target/MS_Job_Board-0.0.1-SNAPSHOT.jar JobMSP-docker.jar
ENTRYPOINT ["java", "-jar", "JobMSP-docker.jar"]