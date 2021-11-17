FROM openjdk:17-alpine

ADD build/libs/*.jar /straightmail.jar

ADD src/main/resources /resources

CMD ["java","-jar","/straightmail.jar"]