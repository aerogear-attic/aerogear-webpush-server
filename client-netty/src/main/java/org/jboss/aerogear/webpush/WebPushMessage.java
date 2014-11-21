package org.jboss.aerogear.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.Optional;

public class WebPushMessage {

    private final Http2Headers headers;
    private final Optional<ByteBuf> payload;

    public WebPushMessage(final Http2Headers headers) {
        this(headers, Optional.empty());
    }

    public WebPushMessage(final Http2Headers headers, final Optional<ByteBuf> payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public Http2Headers headers() {
        return headers;
    }

    public boolean hasData() {
        return payload.isPresent();
    }

    public Optional<ByteBuf> payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "WebPushMessage[header=" + headers + ", payload=" + payload + "]";
    }
}
