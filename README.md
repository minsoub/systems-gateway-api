# Spring Cloud Gateway 사용하기
## 기본 개념
- Route : 게이트웨이의 기본 요소. 라우트, 목적지 URI, 조건자 목록과 필터의 목록을 식벼하기 위한 고유 ID로 구성. 라우트는 모든 조건자가 충족되었을 때 매칭된다.
- Predicates : 각 요청을 처리하기 전에 실행되는 로직. 헤더와 입력값 등 다양한 HTTP 요청이 정의된 기준에 맞는지 찾는다. 구현은 java.util.function.Predicate<T> Java 8 인터페이스 기반으로 한다. 그에 따라 입력 타입은 스프링의 org.springframework.web.server.ServerWebExchange에 기반한다.
- Filter : 들어오는 HTTP 요청 또는 나가는 HTTP 응답을 수정할 수 있게 한다. 다운 스트림 요청을 보내기 전이나 후에 수정할 수 있다. 라우트 필터는 특정 라우트에 한정된다. 스프링의 org.springframework.web.server.GatewayFilter를 구현한다.   

## 프로젝트에 Spring Cloud Gateway 사용하기 
스프링 클라우드 게이트웨이는 Netty 웹 컨테이너와 리액터 프레임워크상에서 개발되었다. 리액터 프로젝트와 스프링 웹플럭스 (Webflux)는 스프링 부트 2.0 버전과 함께 사용할 수 있다.   
스프링 클라우드 게이트웨이를 사용하기 위해서 아래와 같이 gradle.build에 프로젝트를 생성하였다.   
```groovy
plugins {
    id 'org.springframework.boot' version '2.6.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'war'
}

group 'com.bithumbsystems'
version '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR8'
    }
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
}

test {
    useJUnitPlatform()
}
```
## 내장된 조건자와 필터
스프링 클라우드 게이트웨이는 수많은 라우트 조건자와 게이트웨이 필터 팩토리를 내장하고 있다. 모든 라우트는 application.yaml 파일 configuration attribution을 사용하거나 Fluent Java Routes API를 사용해 프로그래밍 방식으로 정의할 수 있다. 사용할 수 있는 조건자 팩토리의 목록은 아래와 같다. 다수의 팩토리가 and 논리 관계를 사용해 단일 라우트 정의에 연결될 수 있다. 필터의 목록은 application.yaml 파일의 spring.cloud.gateway.routes 속성 아래 정의된 각 라우트 아래의 predicates 속성에 구성된다.    
- 조건자 팩토리 목록
   - After Route : Date-time 입력값을 받아서 그 이후에 발생한 요청을 매칭 After=2017-11-20T
   - Before Route : Date-time 입력값을 받아서 그 전에 발생한 요청을 매칭  Before=2019-11-11T
   - Between Route : 두개의 date-time 입력값을 받아서 두 날짜 사이에 발생한 요청을 매칭
   - Cookie Route : 쿠키 이름과 정규식을 입력값으로 받아서 HTTP 요청의 헤더에서 쿠키를 찾고 그 값을 제공된 표현식과 비교한다. Cookie=SessionID, ...
   - Header Route : 헤더 이름과 정규식을 입력값으로 받아서 HTTP 요청의 헤더에서 특정 헤더를 찾고 그 값을 제공된 표현시과 비교  Header=X-Request-Id, ...
   - Host Route : . 구분자를 사용하는 호스트 이름 NAT 스타일 패턴을 입력받아 Host 헤더와 매칭  Host=abc.co.kr
   - Method Route : HTTP 메서드를 입력값으로 받아서 비교한다.  Method=GET
   - Path Route : 요청 컨텍스트 경로의 패턴을 입력값으로 받아 비교  Path=/method/{id}
   - Query Route : 두 개의 입력값 - 요청된 입력값과 선택적 regex를 받아 질의 입력값과 비교 Query=accountId, 1..
   - RemoteAddr Route : IP 주속 목록을 192.168.0.1/16 과 같은 CIDR 표현식으로 받아 요청의 원격 주소와 비교 RemoteAddr=192.168.1.1/16   
기본으로 제공되는 게이트웨이 필터 패턴의 구현이 몇 가지 더 있다. 아래는 사용 가능한 팩토리 목록이다. 필터의 목록은 application.yml 파일의 spring.cloud.gateway.routes 속성 아래 filters 속성에 정의된 각 라우트에 정의할 수 있다.
- 필터 패턴의 팩토리 목록
   - Add RequestHeader : 입력값에 제공된 이름과 값으로 HTTP 요청에 헤더를 추가  AddRequestHeader=X-Response-ID, 123
   - AddRequestParameter : 입력값에 제공된 이름과 값으로 HTTP 요청에 질의 입력값을 추가  AddRequestParameter=id, 123
   - AddResponseHeader : 입력값에 제공된 이름과 값으로 HTTP 응답에 헤더를 추가  AddResponseHeader=X-Response-ID, 123
   - Hystrix : 히스트릭스 명령(HystrixCommand) 이름의 입력값을 받는다  Hystrix=account-service
   - PrefixPath : 입력값에 정의된 접두사를 HTTP 요청 경로에 추가  PrefixPath=/api
   - **RequestRateLimiter** : 제공된 세 개의 입력값에 근거해 단일 사용자의 요청 처리 수를 제한한다. 세 개의 입력값은 초당 최대 요청수, 초당 최대 요청 처리 용량, 사용자 키를 반환하는 빈을 나타낸다.  RequestRateLimiter=10, 20, #{@userKeyResolver}
   - RedirectTo : HTTP 상태 코드와 리다이렉트 경로를 입력값으로 받아서 리다렉트를 수행하기 위해 Location HTTP 헤더에 추가  RedirectTo=30, http://localhost:8080
   - RemoteNonProxyHeaders : 전달된 요청세ㅓ Keep-Alive, Proxy-Authenticate 또는 Proxy-Authorization 등과 같은 몇 가지 hop-by-hop 헤더를 제거한다. 
   - RemoveRequestHeader : 헤더의 이름을 입력값으로 받아 HTTP 요청에서 그것을 제거한다.  RemoveRequestHeader=X-Request-Foo
   - RemoveResponseHeader : 헤더의 이름을 입력값으로 받아 HTTP 응답에서 그것을 제거한다. RemoveResponseHeader=X-Response-ID
   - ReqritePath : Regex 입력값과 대체 입력값을 받아서 요청 경로를 재작성한다. RewirtePath=/account/ (?<path>*),/$\{path}
   - SecureHeaders : 몇 가지 보안 헤더를 응답에 추가한다.
   - SetPath : 경로 template 입력값을 사용하는 단일 입력값을 받아 요청 경로를 변경한다. SetPath=/{segment}
   - SetResponseHeader : 이름과 값을 입력값으로 받아 HTTP 응답에 헤더를 추가한다. SetResponseHeader=X-Response-ID, 123
   - SetStatus : 유효한 HTTP 상태 입력값 하나를 받아 응답에 설정한다.  SetStatus=401   
<br>
아래는 두 개의 조건자와 두 개이 필터를 설정하는 간단한 예제이다. 게이트웨이로 들어오는 각 GET /account/{id} 요청은 http://localhost:8080/api/account/{id]에 새로운 X-Request-ID 헤더를 포함해 전달된다.   
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: example_route
          uri: http://localhost:8080
          predicates:
            - Method=GET
            - Path=/account/{id}
          filters:
            - AddRequestHeader=X-Request-ID, 123
            - PrefixPath=/api
```
동일한 Confiruation을 Route 클래스에 정의된 풍부한 API를 사용해 제공할 수도 있다.  이 방식은 좀 더 자유롭게 사용될 수 있다.  YAML을 사용한 Configuration은 논리 and를 사용하는 조건자의 조합인 반면, 풍부한 자바 API에서는 Predicate 클래스에 있는 and(), or(), negate() 오퍼레이터를 사용한다. 
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder routeBuilder) {
    return routeBuilder.route()
        .route(r -> r.method(HttpMethod.GET).and().path("/account/{id}")
           .addRequestHeader("X-Request-ID", "123").prefixPath("/api")
           .uri("http://localhost:8080"))
        .build();
}
```

## 마이크로 서비스를 위한 게이트웨이
아래 구성은 Path Route Predicate Factory와 RewritePath GatewayFilter Factory를 사용한 예제이다. 경로 재작성 메커니즘은 일부를 추출하거나 몇 가지 패턴을 추가해 요청 경로를 변경한다. 
```yaml
server:
  port: ${PORT:8080}
spring:
  application:
    name: SpringGatewayApi
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://localhost:8091
          predicates:
            - Path=/auth/**
          filters:
            - RewritePath=/auth/(?<path>.*), /$\{path}
        - id: order-service
          uri: http://localhost:8092
          predicates:
            - Path=/order/**
          filters:
            - RewritePath=/order/(?<path>.*), /$\{path}
        - id: product-service
          uri: http://localhost:8093
          predicates:
            - Path=/product/**
          filters:
            - RewritePath=/product/(?<path>.*), /$\{path}
```
auth-service의 경우를 설명하면 http://localhost:8080/auth/login 으로 들어오면 predicates의 조건의 Path 조건에 걸려 필터의 RewritePath로 인해 /auth/login path가 /login path로 변경되어 http://localhost:8091/login으로 재작성되어 전달된다.   
<br>
테스트를 위한 메인 클래스 
```java
package com.bithumbsystems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringGatewayApplication.class, args);
    }
}
```
**troubleshooting**    
- java.lang.NoClassDefFoundError: org/springframework/boot/Bootstrapper
Spring cloud version과 Spring boot version이 맞지 않아 발생하였다.   
아래 버전을 확인하자.   
   - Spring Cloud Release Train
     2020.0x aka IIford : Boot Version 2.4.x, 2.5.x (Starting with 2020.0.3)   
     Hoxton : Boot Version 2.2.x, 2.3.x (Staring with SR5)   
     Greenwich : 2.1.x   
- Spring Boot 2.5.3과 Spring Cloud 2020.0.0과 했을 때 아래와 같이 에러가 발생.
```shell
Description:

Your project setup is incompatible with our requirements due to following reasons:

- Spring Boot [2.5.3] is not compatible with this Spring Cloud release train


Action:

Consider applying the following actions:

- Change Spring Boot version to one of the following versions [2.3.x, 2.4.x] .
You can find the latest Spring Boot versions here [https://spring.io/projects/spring-boot#learn]. 
If you want to learn more about the Spring Cloud Release train compatibility, you can visit this page [https://spring.io/projects/spring-cloud#overview] and check the [Release Trains] section.
If you want to disable this check, just set the property [spring.cloud.compatibility-verifier.enabled=false]
```
수정된 gradle.build 버전   
- spring boot version : 2.4.3
- spring cloud version : 20.0.0
```groovy
plugins {
    id 'org.springframework.boot' version '2.4.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'war'
}

group 'com.bithumbsystems'
version '1.0-SNAPSHOT'
targetCompatibility = "13"

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}
apply plugin: 'io.spring.dependency-management'
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2020.0.0'
    }
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
}

test {
    useJUnitPlatform()
}

//project (':SpringGatewayApi') {
//    dependencies {
//        implementation("org.springframework.cloud:spring-cloud-starter-gateway")
//    }
//}
```
## 전역 필터를 적용한 Spring Cloud Gateway(SCG) 개발하기 
```yanml
# Spring Cloud Gateway server port
server:
  port: 8080

# Spring cloud gateway setup
spring:
  cloud:
    gateway:
      # gateway common filter
      default-filters:
        - name: GlobalFilter
          args:
            baseMessage: Spring Cloud Gateway GlobalFilter
            preLogger: true
            postLogger: true
      # Route definition
      # ID, Destination(uri), Predicate, filter definition
      routes:
        - id: auth-service
          # destination
          uri: http://localhost:9091
          # Condition
          predicates:
            - Path=/user/**
          # Filter
          filters:
            - name: UserFilter
              args:
                baseMessage: UserFilter apply
                preLogger: true
                postLooger: true
        - id: shop-service
          uri: http://localhost:9092
          predicates:
            - Path=/shop/**
          filters:
            - name: ShopFilter
              args:
                baseMessage: ShopFilter apply
                preLogger: true
                postLogger: true
```
default-filters : 전역 필터를 설정한다.   
routes: SCG의 Route 설정을 담당. predicate의 조건 구문을 통해 uri로 라우팅하는데 필터를 직접 구현하거나 Spring이 제공하는 필터를 사용해도 된다.   
위의 설정은 http://localhost:8080/user 호출 시 path=/user/** 조건에 의해 http://localhost:9091/user 로 라우팅 될 것이고, 그 과정에서 GlobalFilter를 거쳐 UserFilter를 거치게 된다.   
<br>
위에서 정의한 Filter의 명칭에 대해서는 클래스로 구현해야 한다.   
- GlobalFilter
```java
package com.bithumbsystems.filter;

import com.bithumbsystems.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UserFilter extends AbstractGatewayFilterFactory<Config> {

  public UserFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {
      log.info("UserFilter baseMessage: {}", config.getBaseMessage());

      if (config.isPreLooger()) {
        log.info("UserFilter Start: {}", exchange.getRequest());
      }

      return chain.filter(exchange).then(Mono.fromRunnable(()-> {
        if (config.isPostLogger()) {
          log.info("UserFilter End: {}", exchange.getResponse());
        }
      }));
    };
  }
}
```
AbstractGatewayFilterFactory: GatewayFiltr 구현을 위한 추상클래스   
exchange : ServerWebExchange 인스턴스로, HTTP 액세스를 제공한다. (요청/응답에 대한 접근 등)   
GatewayFilter (exchange, chain)를 통하여 사용가능하다.    
(exchange, chain) 이후로 request에 대한 접근을 하고 chain.filter(exchange)를 통하여 response를 얻은 후의 접근은 .then(Monfo.fromRunnable 이후로 한다.)
- UserFilter
- ShopFilter    

## Service class
- Port : 9091
```yaml
 server:
   port: 9091
```
- build.gradle
```groovy
plugins {
    id 'org.springframework.boot' version '2.4.3'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
    id 'war'
}

group 'com.bithumbsystem'
version '1.0-SNAPSHOT'
targetCompatibility = "13"

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}
apply plugin: 'io.spring.dependency-management'
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2020.0.0'
    }
}

dependencies {
    implementation('org.springframework.boot:spring-boot-starter-webflux')
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.projectlombok:lombok")
    //implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude group: ("org.junit.vintage"), module: ("junit-vintage-engine")
    }
}

test {
    useJUnitPlatform()
}
```
- java source
```java
package com.bithumbsystems.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController
{
    @GetMapping("/login")
    public Mono<String> login(final ServerHttpRequest request,
                            final ServerHttpResponse response) {
        log.info("User Start");
        final HttpHeaders httpHeaders = request.getHeaders();
        httpHeaders.forEach((key, values) -> log.info("{}: {}", key, values));

        log.info("User End");

        return Mono.just("User Response");
    }
}
```