/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.webpush.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2OrHttpChooser;
import org.jboss.aerogear.webpush.WebPushServer;

import javax.net.ssl.SSLEngine;

/**
 * Negotiates with the browser if HTTP2 or HTTP is going to be used. Once decided, the Netty
 * pipeline is setup with the correct handlers for the selected protocol.
 */
public class Http2OrHttpHandler extends Http2OrHttpChooser {

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private final WebPushServer webPushServer;

    public Http2OrHttpHandler(final WebPushServer webPushServer) {
        this(MAX_CONTENT_LENGTH, webPushServer);
    }

    public Http2OrHttpHandler(int maxHttpContentLength, final WebPushServer webPushServer) {
        super(maxHttpContentLength);
        this.webPushServer = webPushServer;
    }

    @Override
    protected SelectedProtocol getProtocol(final SSLEngine engine) {
        final String[] protocols = engine.getSession().getProtocol().split(":");
        if (protocols.length > 1) {
            SelectedProtocol selectedProtocol = SelectedProtocol.protocol(protocols[1]);
            System.err.println("Selected Protocol is " + selectedProtocol);
            return selectedProtocol;
        }
        return SelectedProtocol.UNKNOWN;
    }

    @Override
    protected ChannelHandler createHttp1RequestHandler() {
        return new WebPushHttp11Handler(webPushServer);
    }

    @Override
    protected Http2ConnectionHandler createHttp2RequestHandler() {
        return new WebPushHttp2Handler(webPushServer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!"javax.net.ssl.SSLException: Received fatal alert: unknown_ca".equals(cause.getMessage())) {
            ctx.fireExceptionCaught(cause);
        }
    }

}
