FROM openjdk:14
ADD build/libs/*.jar /root/app.jar
ADD src/main/resources /root/resources
CMD ["java","-jar","/root/app.jar"]