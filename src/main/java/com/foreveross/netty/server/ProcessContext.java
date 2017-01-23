/*******************************************************************************
 * Copyright (c) Sep 25, 2016 @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a>.
 * All rights reserved.
 *
 * Contributors:
 *     <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> - initial API and implementation
 ******************************************************************************/
package com.foreveross.netty.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.iff.infra.util.Assert;
import org.iff.infra.util.Exceptions;
import org.iff.infra.util.HttpHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.AsciiString;

/**
 * @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> 
 * @since Sep 25, 2016
 */
public class ProcessContext {

	private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
	private static final AsciiString CONTENT_DISPOSITION = new AsciiString("Content-Disposition");
	private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
	private static final AsciiString CONNECTION = new AsciiString("Connection");
	private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

	private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk

	private Properties config;
	private ChannelHandlerContext ctx;
	private HttpRequest request;
	private HttpResponse response;
	private Object msg;
	private byte[] content;
	private String contextPath;
	private String uri;
	private String requestPath;
	private String httpMethod;
	private Map<String, String> headers;
	private Map<String, List<String>> queryParam;
	private Tuple<Map<String, List<String>>, Map<String, File>> postData;
	private Cookie cookie;
	private ByteBuf outputBuffer;
	private List<Cookie> outputCookie;
	private Map<String, Object> attributes;
	private boolean hasInvokeOutput = false;

	public static ProcessContext create(Properties config, ChannelHandlerContext ctx, HttpRequest request, Object msg,
			String contextPath) {
		ProcessContext context = new ProcessContext();
		{
			Assert.notNull(config);
			Assert.notNull(ctx);
			Assert.notNull(request);
			Assert.notNull(request);
			Assert.notBlank(contextPath);
		}
		{
			context.config = config;
			context.ctx = ctx;
			context.request = request;
			context.msg = msg;
			context.outputBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(1024);
			context.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					context.outputBuffer);
			context.contextPath = contextPath;
			if ("POST".equalsIgnoreCase(request.method().name()) && request instanceof FullHttpRequest) {
				ByteBuf content = ((FullHttpRequest) request).content();
				byte[] bs = new byte[content.readableBytes()];
				content.getBytes(content.readerIndex(), bs);
				context.content = bs;
			}
			if (context.content == null) {
				context.content = new byte[0];
			}
		}
		return context;
	}

	public ProcessContext outputHtml() {
		return output("text/html; charset=utf-8");
	}

	public ProcessContext outputText() {
		return output("text/plain; charset=utf-8");
	}

	public ProcessContext outputJson() {
		return output("application/json; charset=utf-8");
	}

	public ProcessContext output(String contextType) {
		setHasInvokeOutput(true);
		contextType = StringUtils.defaultString(contextType, "text/plain");
		getOutputHeaders().set(CONTENT_TYPE, contextType);
		getOutputHeaders().setInt(CONTENT_LENGTH, outputBuffer.readableBytes());
		if (!HttpUtil.isKeepAlive(request)) {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			getOutputHeaders().set(CONNECTION, KEEP_ALIVE);
			ctx.write(response);
		}
		return this;
	}

	public boolean isHasInvokeOutput() {
		return hasInvokeOutput;
	}

	public ProcessContext setHasInvokeOutput(boolean hasInvokeOutput) {
		this.hasInvokeOutput = hasInvokeOutput;
		return this;
	}

	public ProcessContext outputFile(String fileName) {
		fileName = StringUtils.defaultString(fileName, "nofilename");
		try {
			if (Boolean.TRUE.equals(HttpHelper.userAgent(getHeaders().get("User-Agent")).get("isIE"))) {
				fileName = URLEncoder.encode(fileName.replaceAll(" ", ""), "UTF-8");
			} else {
				fileName = new String(fileName.replaceAll(" ", "").getBytes("UTF-8"), "ISO8859-1");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		getOutputHeaders().set(CONTENT_TYPE, "application/octet-stream");
		getOutputHeaders().set(CONTENT_DISPOSITION, "attachment;filename=" + fileName);
		getOutputHeaders().setInt(CONTENT_LENGTH, outputBuffer.readableBytes());
		if (!HttpUtil.isKeepAlive(request)) {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			getOutputHeaders().set(CONNECTION, KEEP_ALIVE);
			ctx.write(response);
		}
		return this;
	}

	public ProcessContext addAttribute(String name, Object value) {
		if (attributes == null) {
			attributes = new LinkedHashMap<String, Object>();
		}
		attributes.put(name, value);
		return this;
	}

	public Map<String, Object> getAttributes() {
		if (attributes == null) {
			attributes = new LinkedHashMap<String, Object>();
		}
		return attributes;
	}

	public ProcessContext addCookie(Cookie cookie) {
		if (cookie == null) {
			return this;
		}
		if (outputCookie == null) {
			outputCookie = new ArrayList<Cookie>();
		}
		outputCookie.add(cookie);
		return this;
	}

	public Cookie getCookie() {
		if (cookie == null) {
			String value = request.headers().get(HttpHeaderNames.COOKIE);
			if (value != null) {
				cookie = ClientCookieDecoder.LAX.decode(value);
			}
		}
		return cookie;
	}

	public String getContentAsString() {
		if (content.length > 0) {
			try {
				return new String(content, "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getContentAsString(String encoding) {
		if (content.length > 0) {
			try {
				return new String(content, encoding);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public HttpHeaders getOutputHeaders() {
		return response.headers();
	}

	public ByteBuf getOutputBuffer() {
		return outputBuffer;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public byte[] getContent() {
		return content;
	}

	public Properties getConfig() {
		return config;
	}

	public String getUri() {
		if (uri == null) {
			uri = request.uri();
		}
		return uri;
	}

	public String getRequestPath() {
		if (requestPath == null) {
			requestPath = StringUtils.substringBefore(getUri(), "?");
		}
		return requestPath;
	}

	public String getHttpMethod() {
		if (httpMethod == null) {
			httpMethod = request.method().name();
		}
		return httpMethod;
	}

	public Map<String, String> getHeaders() {
		if (headers == null) {
			headers = new LinkedHashMap<String, String>();
			for (Entry<String, String> entry : request.headers().entries()) {
				headers.put(StringUtils.lowerCase(entry.getKey()), entry.getValue());
			}
			String ip = getIpAddr(headers);
			if (StringUtils.isBlank(ip)) {
				SocketAddress remoteAddress = ctx.channel().remoteAddress();
				if (remoteAddress instanceof InetSocketAddress) {
					InetSocketAddress insocket = (InetSocketAddress) remoteAddress;
					ip = insocket.getAddress().getHostAddress();
				}
			}
			if (StringUtils.isNotBlank(ip)) {
				headers.put("client_ip", ip);
			}
		}
		return headers;
	}

	private String getIpAddr(Map<String, String> headers) {
		String ip = "";
		String mark = headers.get("proxy-enable");
		if (mark == "1") {
			ip = headers.get("x-forwarded-for");
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = headers.get("proxy-client-ip");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = headers.get("wl-proxy-client-ip");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = "";
			}
		}
		return ip;
	}

	public Map<String, List<String>> getQueryParam() {
		if (queryParam == null) {
			queryParam = new QueryStringDecoder(getUri()).parameters();
		}
		return queryParam;
	}

	public Tuple<Map<String, List<String>>, Map<String, File>> getPostData() {
		if (postData == null) {
			postData = processPostData(new HttpPostRequestDecoder(factory, request));
		}
		return postData;
	}

	public String getContextPath() {
		return contextPath;
	}

	private boolean hasNextDecoder(HttpPostRequestDecoder decoder) {
		boolean hasNext = false;
		try {
			hasNext = decoder.hasNext();
		} catch (Exception e) {
		}
		return hasNext;
	}

	protected Tuple<Map<String, List<String>>, Map<String, File>> processPostData(HttpPostRequestDecoder decoder) {
		Map<String, List<String>> requestParameters = new HashMap<String, List<String>>();
		Map<String, File> requestFiles = new HashMap<String, File>();

		try {
			if (msg instanceof HttpContent) {
				HttpContent chunk = (HttpContent) msg;
				decoder.offer(chunk);
				while (hasNextDecoder(decoder)) {
					InterfaceHttpData data = decoder.next();
					if (data == null) {
						continue;
					}
					try {
						if (data.getHttpDataType() == HttpDataType.Attribute) {
							Attribute attribute = (Attribute) data;
							List<String> values = requestParameters.get(attribute.getName());
							if (values == null) {
								values = new ArrayList<String>();
								requestParameters.put(attribute.getName(), values);
							}
							values.add(attribute.getValue());
						} else if (data.getHttpDataType() == HttpDataType.FileUpload) {
							FileUpload fileUpload = (FileUpload) data;
							if (fileUpload.isCompleted() && fileUpload.length() > 0) {
								File tempFile = File.createTempFile("agent", "upload");
								fileUpload.renameTo(tempFile);
								requestFiles.put(fileUpload.getName(), tempFile);
							}
						}
					} finally {
						data.release();
					}
				}
			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
			Exceptions.runtime("Decode post data error!", ioe);
		}
		return Tuple.of(requestParameters, requestFiles);
	}
}
