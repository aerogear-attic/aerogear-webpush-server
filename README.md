# Aerogear WebPush Server 
This project is a Java server side implementation of the draft [WebPush Protocol](http://tools.ietf.org/html/draft-thomson-webpush-http2-01) 
specification.

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
