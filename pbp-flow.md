# flow builder

```java
private void prepareInboundContext(RouteConfig routeConfig, Exchange exchange) {
    log.info("prepare inbound context ...");
    
    // Get file details before clearing the message
    String sourcePath = exchange.getIn().getHeader(Exchange.FILE_PATH, String.class);
    String sourceName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
    
    // Clear the message body to free up memory
    exchange.getIn().setBody(null);
    
    // Create execution context with necessary data
    InboundContext routeContext = new InboundContext();
    routeContext.setRouteConfig(routeConfig);
    routeContext.setSourcePath(sourcePath);
    routeContext.setSourceName(sourceName);
    routeContext.setFormat("json");
    LocalDateTime now = LocalDateTime.now();
    routeContext.setStart(now.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli());
            
    // Store only the context in exchange property
    exchange.setProperty("routeContext", routeContext);

    log.info("routeContext: {}", routeContext);
}
```
