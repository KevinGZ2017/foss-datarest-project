/*******************************************************************************
 * Copyright (c) Sep 25, 2016 @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a>.
 * All rights reserved.
 *
 * Contributors:
 *     <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> - initial API and implementation
 ******************************************************************************/
package com.foreveross.netty.server;

import com.foreveross.datarest.core.service.HttpRequestHandler;
import com.foreveross.datarest.core.service.HttpStaticHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.iff.infra.util.Assert;

/**
 * http server initializer.
 * @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> 
 * @since Sep 25, 2016
 */
public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

	protected HttpServer server;
	protected SslContext sslCtx;
	protected boolean sslEnable;

	public HttpServerInitializer(HttpServer server, SslContext sslCtx, boolean sslEnable) {
		super();
		Assert.notNull(server);
		Assert.notNull(sslCtx);
		this.server = server;
		this.sslCtx = sslCtx;
		this.sslEnable = sslEnable;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if (sslEnable) {
			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}
		ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(1024*1024*64));
        ch.pipeline().addLast(new ChunkedWriteHandler());
		ch.pipeline().addLast(new HttpStaticHandler());
		ch.pipeline().addLast(new HttpRequestHandler("/index"));
		ch.pipeline().addLast(new HttpObjectAggregator(65536));
		ch.pipeline().addLast(new HttpServerInboundHandler(server));
	}

}