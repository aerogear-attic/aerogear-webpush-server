# Aerogear WebPush Server [![Build Status](https://travis-ci.org/aerogear/aerogear-webpush-server.png)](https://travis-ci.org/aerogear/aerogear-webpush-server)
This project is a proof of concept implementation of the 
[WebPush Protocol](http://tools.ietf.org/html/draft-thomson-webpush-http2-02) specification with the purpose of gaining
a better understanding of the specification. IETF [charter](https://datatracker.ietf.org/wg/webpush/charter)

|                 | Project Info  |
| --------------- | ------------- |
| License:        | Apache License, Version 2.0  |
| Build:          | Maven  |
| Documentation:  | https://aerogear.org/push/  |
| Issue tracker:  | https://issues.jboss.org/browse/AGPUSH  |
| Mailing lists:  | [aerogear-users](http://aerogear-users.1116366.n5.nabble.com/) ([subscribe](https://lists.jboss.org/mailman/listinfo/aerogear-users))  |
|                 | [aerogear-dev](http://aerogear-dev.1069024.n5.nabble.com/) ([subscribe](https://lists.jboss.org/mailman/listinfo/aerogear-dev))  |

## Message batching/aggregation
Allows an application to request that a web push server deliver the same message to a potentially large set of devices,
and is a separate specification called [webpush-aggregate](http://tools.ietf.org/html/draft-thomson-webpush-aggregate-00)

## Prerequisites 
* This project requires Java 8.

AeroGear WebPush consists of the following modules:

* [common](./common)  
Just common classes used by multiple modules in the project.

* [datastores](./datastores)  
Contains implementations of various datastores. Please see the specific datastore's readme for further details.

* [protocol](./protocol)  
The WebPush Server Protocol provides interfaces for the protocol.

* [server-api](./server-api)  
An API for AeroGear WebPush Server

* [server-core](./server-core)  
An implementation of AeroGear WebPush Server API.

* [server-netty](./server-netty)  
The WebPush Server implementation that uses Netty 5.x.

* [client-netty](./client-netty)  
The WebPush Client implementation that uses Netty 5.x.

Please refer to the above modules documentation for more information.
