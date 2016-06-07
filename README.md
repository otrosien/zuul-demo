# Zuul Proxy Timeout Evaluation.

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

1) non-ribbon mode. Simplified, uses jersey-client/apache httpclient, only uses low-level socket timeout.

defaults:
* socket-timeout: 10s
* connect-timeout: 2s

configuration:
```
zuul:
  host:
    socket-timeout-millis: 500
    connect-timeout-millis: 200
```
see: org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter


2) ribbon mode

"Normal modus operandi" for Netflix. Used when `serviceId` is set.
Need to force Eureka off (ribbon.eureka.enabled: false) Uses Apache httpclient / (legacy ribbon RestClient)
behind a client-side load-balancing algorithm. (com.netflix.client.AbstractLoadBalancerAwareClient)

(1) general hystrix timeout around all calls

defaults:
* 1000ms

configuration:
```
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 1000
```

(2) connect timeout and read timeouts global / per named ribbon service

defaults:
* ReadTimeout: 5000ms
* ConnectTimeout: 2000ms
* MaxAutoRetries: 0
* MaxAutoRetriesNextServer: 1

configuration:
```
ribbon:
    ConnectTimeout: 5000
    ReadTimeout: 1500

backendService:
  ribbon:
    ReadTimeout: ....

```

Total number of requests: (1 + MaxAutoRetries) * (1 + MaxAutoRetriesNextServer)

see:
* com.netflix.client.DefaultLoadBalancerRetryHandler
* com.netflix.client.config.DefaultClientConfigImpl



