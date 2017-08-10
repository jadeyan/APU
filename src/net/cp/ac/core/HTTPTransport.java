/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.Transport;
import net.cp.syncml.client.util.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 * A class implementing a HTTP transport for SyncML on Android.
 *
 * @author James O'Connor
 */
public class HTTPTransport implements Transport {
    // defines the HTTP headers that we use
    public static final String HEADER_CONNECTION = "Connection";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_DISP = "Content-Disposition";
    public static final String HEADER_USER_AGENT = "User-Agent";

    private final String server, urlpath;
    private String targetUrl;
    private DefaultHttpClient httpClient;
    private final ByteArrayOutputStream outputStream;

    HttpParams httpParams;
    private HttpPost postRequest;
    private final int maxMessageSize;

    private Logger logger;

    private int connectionType;

    private int connectionTimeout;

    private boolean abort_sync;

    /**
     * @param logger The logger to use
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @param connectionType the type of connection to use for this transport
     */
    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    /**
     * Set the timeout for the HTTP connection.
     * An exception will be thrown when the timeout has expired and the connection has not been established.
     *
     * @param timeout how long to wait for the connection to succeed before throwing an exception
     */
    public void setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
        httpClient = null; // make sure we create a new client that uses the new timeout value
    }

    /**
     * @param host The host to connect to
     * @param port The port to use
     * @param path The path to request on the HTTP server
     */
    public HTTPTransport(String host, int port, boolean ssl, String path) {
        this(host, port, ssl, path, 8192);
    }

    /**
     * @param host The host to connect to
     * @param port The port to use
     * @param ssl indicates if SSL should be used when connecting to the server
     * @param path The path to request on the HTTP server
     * @param maxMsgSize The maximum message size
     */
    public HTTPTransport(String host, int port, boolean ssl, String path, int maxMsgSize) {
        this(host, port, ssl, null, 0, path, maxMsgSize);
    }

    /**
     * @param host The host to connect to
     * @param port The port to use
     * @param ssl indicates if SSL should be used when connecting to the server
     * @param proxyHost The proxy host to use, or null for no proxy
     * @param proxyPort The proxy port to use, or 0 for no proxy
     * @param path The path to request on the HTTP server
     * @param maxMsgSize The maximum message size
     */
    public HTTPTransport(String host, int port, boolean ssl, String proxyHost, int proxyPort, String path, int maxMsgSize) {
        server = host;
        urlpath = path;

        if (ssl)
            targetUrl = "https://" + server + ":" + port + urlpath;
        else
            targetUrl = "http://" + server + ":" + port + urlpath;

        maxMessageSize = maxMsgSize;

        httpParams = new BasicHttpParams();

        outputStream = new ByteArrayOutputStream(maxMessageSize);

        abort_sync = false;

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.Transport#getTargetURI()
     */
    @Override
    public String getTargetURI() {
        if (logger != null) logger.info("HTTPTransport: targetUrl " + targetUrl);
        return targetUrl;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.Transport#getMaxMsgSize()
     */
    @Override
    public int getMaxMsgSize() {
        return maxMessageSize;
    }

    @Override
    public InputStream getInputStream() throws IOException, SyncException {
        try {
            long start = System.currentTimeMillis();

            if (logger != null) logger.info("HTTPTransport: start doing to POST targetUrl " + targetUrl);

            // get whatever data we have buffered to send
            byte[] outputData = outputStream.toByteArray();
            if ((outputData == null) || (outputData.length <= 0)) throw new IllegalStateException("no data buffered to send");

            // send it
            postRequest = new HttpPost(targetUrl);
            postRequest.setHeader("Content-Type", Transport.CONTENT_TYPE_WBXML);

            postRequest.setEntity(new ByteArrayEntity(outputData));

            long startCT = System.currentTimeMillis();

            boolean success = AndroidConnectionState.setConnectionType(connectionType, server);

            if (logger != null) logger.info("HTTPTransport: set connection time (time=" + (System.currentTimeMillis() - startCT) + ")");

            if (!success) throw new IOException("Unable to set the connection type to: " + connectionType + " with server: " + server);

            // try to reuse http connection if possible
            if (httpClient == null) {

                long startHttpClient = System.currentTimeMillis();

                // set the timeout
                if (connectionTimeout > 0) {
                    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
                    HttpConnectionParams.setSoTimeout(httpParams, connectionTimeout);

                    // hacks
                    // HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);
                    HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
                    HttpProtocolParams.setUseExpectContinue(httpParams, false);

                    // create the client with the params
                    httpClient = new DefaultHttpClient(httpParams);

                    // java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
                    // java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
                    //
                    // System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
                    // System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
                    // System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
                    // System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
                    // System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");

                    if (logger != null)
                        logger.info("HTTPTransport: Creating HTTPClient with connectionTimeout=" + connectionTimeout + "(time="
                                + (System.currentTimeMillis() - startHttpClient) + ")");

                } else {
                    httpClient = new DefaultHttpClient();

                    if (logger != null)
                        logger.info("HTTPTransport: Creating HTTPClient without connectionTimeout+  (time=" + (System.currentTimeMillis() - startHttpClient)
                                + ")");
                }
            }

            long startExecuting = System.currentTimeMillis();

            // execute the request
            HttpResponse response = httpClient.execute(postRequest);

            if (logger != null) {
                long time = (System.currentTimeMillis() - startExecuting);
                long size = outputData.length;
                long speed = size > 0 && time > 1000 ? (size / 1024) / (time / 1000) : -1;
                logger.info("HTTPTransport: executing POST (time=" + time + ") - size=" + outputData.length + " - speed= "
                        + (speed != -1 ? speed + "kps" : "NaN"));
            }

            outputData = null;

            // check the response

            int requestStatus = response.getStatusLine().getStatusCode();
            if (requestStatus >= 400)
                throw new IllegalStateException("fatal HTTP status: " + requestStatus + " (" + response.getStatusLine().getReasonPhrase() + ")");

            long startInput = System.currentTimeMillis();

            InputStream stream = response.getEntity().getContent();

            if (logger != null) logger.info("HTTPTransport: getting Input POST (time=" + (System.currentTimeMillis() - startInput) + ")");

            if (logger != null)
                logger.info("HTTPTransport: end doing POST to targetUrl " + targetUrl + " (time=" + (System.currentTimeMillis() - start) + ")");

            return stream;
        } catch (Throwable e) {
            if (abort_sync == true) {
                if (logger != null) logger.error("HTTP: Failed to send message to the server - aborting sync", e);

                throw new SyncException("Transport stopped - aborting", SyncML.STATUS_OPERATION_CANCELLED);
            } else {
                if (logger != null) logger.error("HTTP: Failed to send message to the server - giving up", e);

                // this exception indicates a fatal protocol error
                throw new IOException("fatal HTTP protocol error: " + e.getMessage());
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.Transport#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        // buffer all output until an input stream is required
        outputStream.reset();

        return outputStream;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.Transport#getContentType()
     */
    @Override
    public String getContentType() {
        return getHeaderField("Content-Type");
    }

    /**
     * Returns the value of the specified message header.
     *
     * @param header    the name of the header to retrieve.
     * @return the value of the header, or null if there is no request present
     * @throws IOException if the header couldn't be retrieved.
     */
    public synchronized String getHeaderField(String header) {
        if (postRequest != null) {
            Header ctHeader = postRequest.getFirstHeader(header);
            HeaderElement[] ctHeaderElement = ctHeader.getElements();
            return ctHeaderElement[0].getName();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.Transport#cleanup()
     */
    @Override
    public void cleanup() {
        if (postRequest != null) postRequest.abort();

    }

    /**
     * Stops the current HTTP request.
     */
    public void stopTransport() {
        abort_sync = true;
    }

}
