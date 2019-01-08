# Local Setup

### Prerequisites
1. Install Spring Cloud CLI (you will need this to set up eureka and config server locally) - https://cloud.spring.io/spring-cloud-cli/
3. Install Java8 - https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
4. Install Gradle - https://gradle.org/install/ 

## Configure the Config Server
Create a new configuration file for the config server at ~/.spring-cloud/configserver.yml
```
spring:
  profiles:
    active: git
  cloud:
    config:
      server:
        git:
          uri: https://github.com/pivotalbank/cf-SpringBootTrader-config.git
       # native:
       #    searchLocations: "/Users/srowe/workspace/pivotal-bank/cf-SpringBootTrader/cf-SpringBootTrader-config"
```

If you want the config server to use config from your local file system set spring.profiles.active to native, and uncomment the native.searchLocations property

## Starting the config server and eureka
```spring cloud configserver eureka```


## Starting UAA
1. From the root directory of this project run ```./gradlew -b uaa-server/build.gradle uaa ```

## Starting each of the microservices
From the root project directory 
1. Quotes Service - ```./gradlew -b quotes-service/build.gradle bootRun ```
2. User Service - ```./gradlew -b user-service/build.gradle bootRun ```
3. Account Service - ```./gradlew -b account-service/build.gradle bootRun ```
4. Portolio Service - ```./gradlew -b portfolio-service/build.gradle bootRun ```
5. Web UI - ```./gradlew -b web-ui/build.gradle bootRun ```

Then nagivate to http://localhost:8080 and login with user: user/password.