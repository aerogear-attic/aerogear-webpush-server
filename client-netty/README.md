## WebPush Client based on Netty


## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server. 

### Start the server

    cd server-netty
    mvn exec:exec
    (maven output not displayed)
    [main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443
    
### Start the console
Press ```tab``` to see the available commands and also to display the available options for specific commands.   
Most command also support a ```--help``` option to display information about options that the command accepts.
  
    cd client-netty
    mvn exec:exec
    
    [webpush]$

#### Connect

    [webpush]$ connect -h localhost -p 8443
    Connected to [localhost:8443]
    
#### Register 

    [webpush]$ register
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: /webpush/register, :scheme: https]
    < [streamid:3] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, Location, cache-control: private, max-age=604800000, link: <webpush/aggregate/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:aggregate", link: <webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:reg", link: <webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:sub", location: webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45]
    
#### Create a subscription

    [webpush]$ subscribe --url webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45
    > DefaultHttp2Headers[:authority: localhost:8443, :method: POST, :path: webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45, :scheme: https]
    < [streamid:5] DefaultHttp2Headers[:status: 201, access-control-allow-origin: *, access-control-expose-headers: Location, cache-control: private, max-age=604800000, location: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC]
    
#### Monitor    

    [webpush]$ monitor --url webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45
    > DefaultHttp2Headers[:authority: localhost:8443, :method: GET, :path: webpush/reg/14752553-f74c-4031-9e0f-1dbc7a54cb45, :scheme: https]
    < [streamid:7] DefaultHttp2Headers[:status: 200, access-control-allow-origin: *, access-control-expose-headers: Link, Cache-Control, cache-control: private, max-age=604800000, link: <webpush/aggregate/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:aggregate", link: <webpush/subscribe/14752553-f74c-4031-9e0f-1dbc7a54cb45>;rel="urn:ietf:params:push:sub"]
    
#### Send notification

    [webpush]$ notify --url /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC --payload hello
    > DefaultHttp2Headers[:authority: localhost:8443, :method: PUT, :path: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC, :scheme: https]
    < [streamid:2] DefaultHttp2Header]
    < [streamid:2] hello
    [webpush]$ notify --url /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC --payload hello2
    > DefaultHttp2Headers[:authority: localhost:8443, :method: PUT, :path: /webpush/zZ9Y1tf1aSjKF135DlJve4TUcbp33tSfiHsalh8a0U%2FTFLd54bCSiVf0KX9YB2jw6W5lVNcBK3aO25C3ccknfpnMO77qJiUitrG4tvKSyhDmIFQFef8ZOCq9RwI1u8H7%2Bg70U0S79gXC, :scheme: https]
    < [streamid:2] hello2
    
#### Exit the console
    
    > exit
    



