package org.jboss.aerogear.webpush;

/**
 * Allows a WebPush client the ability to handle responses from the WebPush Server.
 */
public interface ResponseHandler {

    /**
     * The response from a registration request.
     *
     * @param channelLink the link to be used to create channels. Currently the complete WebLink header.
     * @param monitorLink the link to be used to start monitoring. Currently the complete WebLink header.
     * @param streamId the streamId for this response.
     */
    void registerResponse(String channelLink, String monitorLink, int streamId);

    /**
     * The response from a channel creation request.
     *
     * @param endpoint the endpoint used by app servers to send (PUT) notifications.
     * @param streamId the streamId for this response.
     */
    void channelResponse(String endpoint, int streamId);

    /**
     * Notifications send from the WebPush server
     *
     * @param data the body of the application server PUT request.
     * @param streamId the streamId for this notification.
     */
    void notification(String data, int streamId);
}
