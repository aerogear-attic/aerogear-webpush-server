## WebPush Client based on Netty


## Using the WebPush console
A very basic console/command line application is available to assist in development and testing of the server. 

The following [screen cast](https://drive.google.com/file/d/0B2E1HZ1JnrJfYW90eVBTaGkzSkU/view?usp=sharing) shows the 
same interactions as below.
 
### Start the server

    cd server-netty
    mvn exec:exec
    (maven output not displayed)
    [main] INFO org.jboss.aerogear.webpush.netty.WebPushNettyServer - WebPush server bound to localhost:8443
    
### Start the console
The console support tab completion and you can type ```help``` to see the available commands.
  
    cd client-netty
    mvn exec:exec
    
    WebPush console
    > 

#### Connect
    WebPush console
    > connect localhost:8443
    Connected to [localhost:8443]
    >
    
#### Register 

    > register
    < [streamid:3] ChannelLink: webpush/d55f7b70-763c-480e-a043-0806fbcbe4f1/channel, MonitorLink: webpush/d55f7b70-763c-480e-a043-0806fbcbe4f1/monitor
    >
    
#### Create a channel
    
    > create-channel webpush/d55f7b70-763c-480e-a043-0806fbcbe4f1/channel
    < [streamid:5] Endpoint: webpush/a453gLsY5hlfXfs3TDzrIbjKUb6oKtoOIP4SX2VCYbIajei%2FaJTp7SgUGGv233sQOkY0v1peXoeun01xg9xQvMG7YAgcfTEEUZeKXS1m2CONLGZZoqfFlqthU4Owcy4MVu4sNN9YaqmI
    >
    
#### Monitor    
    > monitor webpush/d55f7b70-763c-480e-a043-0806fbcbe4f1/monitor
    >
    
#### Send notification
    > notify webpush/a453gLsY5hlfXfs3TDzrIbjKUb6oKtoOIP4SX2VCYbIajei%2FaJTp7SgUGGv233sQOkY0v1peXoeun01xg9xQvMG7YAgcfTEEUZeKXS1m2CONLGZZoqfFlqthU4Owcy4MVu4sNN9YaqmI hello
    < [streamid:2] hello
    >
    
#### Exit the console
    
    > quit
    
    



