# Zuul Proxy Timeout Evaluation

The Zuul Proxy contained in Spring Cloud Zuul comes in two proxy modes. Each of the modes is configured quite differently.
Here are the configuration options. A complete configuration set is in the repository and at the end of this readme.


## 1) Non-ribbon mode

The non-ribbon mode is using a combination of jersey-client and Apache HttpComponents. For timeouts it can only take a
low-level socket timeout. The calls are not backed by hystrix and there is no client-side load balancing possibility.

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

"Normal modus operandi" for Netflix. This mode is picked up when you set a `serviceId` for your zuul route. It is usually used in combination
where the URL endpoints come from a service discovery, like Eureka. If this is not what you want, you need to force
Eureka discovery to be off (`ribbon.eureka.enabled: false`) Under the hood it also uses Apache HttpComponents (or alternatively the legacy Ribbon RestClient),
running behind a client-side load-balancing algorithm. (see com.netflix.client.AbstractLoadBalancerAwareClient)

Timeouts can be applied on two levels.

(1) General Hystrix timeout (applied for the whole transaction) Beware that you should not set this to less than the ribbon read timeout or connect
timeout.

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


## 3) Sample configuration

```
ribbon:
  eureka:
    enabled: false
  ConnectTimeout: 200
  ReadTimeout: 2000
  MaxAutoRetries: 1
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

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 200000

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
