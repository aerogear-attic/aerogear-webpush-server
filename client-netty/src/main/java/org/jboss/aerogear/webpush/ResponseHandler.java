package org.jboss.aerogear.webpush;

import io.netty.handler.codec.http2.Http2Headers;

/**
 * Allows a WebPush client the ability to handle responses from the WebPush Server.
 */
public interface ResponseHandler {

    /**
     * The response from a registration request.
     *
     * @param headers the headers returned from the register request.
     * @param streamId the streamId for this response.
     */
    void registerResponse(final Http2Headers headers, int streamId);

    /**
     * The response from a subscription creation request.
     *
     * @param headers the headers returned from the register request.
     * @param streamId the streamId for this response.
     */
    void subscribeResponse(Http2Headers headers, int streamId);

    /**
     * Notifications send from the WebPush server
     *
     * @param data the body of the application server PUT request.
     * @param streamId the streamId for this notification.
     */
    void notification(String data, int streamId);

    /**
     * The status of a subscription
     *
     * @param headers the headers returned from the register request.
     * @param streamId the streamId for this notification.
     */
    void status(Http2Headers headers, int streamId);

}
