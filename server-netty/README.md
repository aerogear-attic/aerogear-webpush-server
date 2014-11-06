# AeroGear Netty WebPush Server
This project is a Java implementation of the server side that follows the
[WebPush Protocol](http://tools.ietf.org/html/draft-thomson-webpush-http2-00).

## Usage

### Build the WebPush Server

    mvn install

### Start the WebPush Server

    mvn exec:exec
    
This will start the server listening localhost using port 7777. This will use a default configuration which can be found

### Configuration
Configuration is done using JSON configuration file.
Example configuration:  

    {
        "host": "localhost",
        "port": 7777,
        "password" :"testing",
        "endpoint-host": "external.name",
        "endpoint-port": 8899,
        "endpoint-tls": false,
        "endpoint-prefix": "/update",
        "datastore": { "in-memory": {} },
        "protocol": "ALPN"
    }

#### host
The host that the server will bind to.

#### port
The port that the server will bind to.

#### password
This should be a password that will be used to generate the server private key which is used for  encryption/decryption
of the endpoint URLs that are returned to clients upon successful channel registration.

#### endpoint-host
The allows the configuration of the host name that will be exposed for the endpoint that clients use to send notifications.
This enables an externally exposed host name/ip address to be specified which differs from the host that the server 
binds to.

#### endpoint-port
The allows the configuration of the port that will be exposed for the endpoint that clients use to send notifications.
This enables an externally exposed port to be specified which differs from the host that the server binds to.

#### endpoint-tls
Configures Transport Layer Security (TLS) for the notification endpointUrl that is returned when a UserAgent/client registers a channel. 
Setting this to _true_ will return a url with _https_ as the protocol.

#### endpoint-prefix  
The prefix for the the notification endpoint url. This prefix will be included in the endpointUrl returned to the client to enabling them to send notifications.

#### datastore
Configures the datastore to be used.  

InMemory datastore:

    "datastore": { "in-memory": {} }
    


    
    
