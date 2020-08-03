FROM openjdk:14
ADD build/libs/*.jar /app.jar
ADD src/main/resources /resources
CMD ["java","-jar","/app.jar"]