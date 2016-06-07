# Zuul Proxy Timeout Evaluation

The Zuul Proxy contained in Spring Cloud Zuul comes in two proxy modes. Each of the modes is configured quite differently.
Here are the configuration options. A complete configuration set is in the repository and at the end of this readme.

## 0) About this example

There are four applications to start, e.g. via `./gradlew bootRun`.

1. config-server (connects to port 8889) - This needs to be run first, serving configuration for the other services (see `config` folder)
2. backend-server (connects to port 8082) - This is our backend service
3. api-proxy (connects to port 8080) - Our proxy
4. hystrix-dashboard (connects to port 8099) - Monitoring tool

To test out different settings, there are two routes set-up on the api-proxy:

* /ribbon/... for connecting to the backend via ribbon client
* /nonribbon/... for connecting to the backend with plain http client

The backend provides different endpoints for testing out different latency patterns.

* /api/ok - responds immediately
* /api/1s - responds in one second
* /api/random - responds with random delay between 0 and 1 second
* /api/sleep/{millis} - responds with delay configurable via path param
* /api/post/{millis} - same, but using a `POST` method

## 1) Non-ribbon mode

The non-ribbon mode is using a combination of jersey-client and Apache HttpComponents. For timeouts it can only take a
low-level socket timeout. The calls are not backed by Hystrix and there is no client-side load balancing possibility.

Defaults:

* socket-timeout: 10.000 ms
* connect-timeout: 20.000 ms

Configuration example:

```
zuul:
  host:
    socket-timeout-millis: 500
    connect-timeout-millis: 200
```

See: org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter


## 2) Ribbon mode

This mode is picked up when you set a `serviceId` for your zuul route. It is usually used in combination
where the URL endpoints come from a service discovery, like Eureka. If this is not what you want, you need to force
Eureka discovery to be off (`ribbon.eureka.enabled: false`) Under the hood it also uses Apache HttpComponents (or alternatively the legacy Ribbon RestClient),
running behind a client-side load-balancing algorithm. (see com.netflix.client.AbstractLoadBalancerAwareClient)

Timeouts can be applied on two levels.

(1) General Hystrix timeout applied for the whole transaction. This would be your guaranteed response time.
Beware that it does not make much sense to set this to less than the ribbon read timeout.

Defaults:
* 1000 ms

Configuration example:

```
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 1000
```

(2) Ribbon connect and read timeouts global or per named ribbon service

Defaults:

* ReadTimeout: 5000 ms
* ConnectTimeout: 2000 ms
* MaxAutoRetries: 0
* MaxAutoRetriesNextServer: 1

Configuration example:

```
ribbon:
    ConnectTimeout: 5000
    ReadTimeout: 1500

backendService:
  ribbon:
    ReadTimeout: ....

```

Total number of requests: `(1 + MaxAutoRetries) * (1 + MaxAutoRetriesNextServer)`

Intrestingly, by default there is one retry, as `MaxAutoRetriesNextServer` kicks in even if there is no second URL specified.

See:
* com.netflix.client.DefaultLoadBalancerRetryHandler
* com.netflix.client.config.DefaultClientConfigImpl


## 3) Caveats

Netflix is deprecating the Ribbon library (as stated here: https://github.com/Netflix/ribbon/issues/248), so in the
future, i.e. Spring 5, Spring Cloud would integrate rxnetty as a client replacement (stated here https://github.com/spring-cloud/spring-cloud-netflix/issues/961)

## 4) Sample configuration

```
ribbon:
  eureka:
    enabled: false
  ConnectTimeout: 200
  ReadTimeout: 2000
  MaxAutoRetries: 2
  MaxAutoRetriesNextServer: 0

zuul:
  routes:
    backendRibbon:
      path: /ribbon/**
      stripPrefix: true
      serviceId: backendRibbon
    backendNonRibbon:
      path: /nonribbon/**
      stripPrefix: true
      url: http://localhost:8082/
  host:
    socket-timeout-millis: 10000
    connect-timeout-millis: 20000

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000

backendRibbon:
  ribbon:
    listOfServers: localhost:8082
#    MaxAutoRetries: 5
#    ConnectTimeout: 1500
#    ReadTimeout: 1500
```

Things to try out:

* http://localhost:8080/ribbon/api/sleep/5000
* http://localhost:8080/nonribbon/api/sleep/5000
