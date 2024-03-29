/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;


import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;

import net.cp.syncml.client.*;


/**
 * A class implementing HTTP transport for SyncML.
 * 
 * This implementation requires <a href=http://jakarta.apache.org/httpcomponents/httpclient-3.x>Jakarta Commons HTTP Client v3.1</a>
 *
 * @author Denis Evoy
 */
public class HTTPTransport implements Transport
{
    private URI targetUri;
    private HttpClient httpClient;
    private ByteArrayOutputStream outputStream;
    private PostMethod postRequest;
    private int maxMessageSize;

    private int inputCount;
    private int outputCount;
    private int syncErrorOutputCount;
    private int syncErrorInputCount;
    
    private Hashtable extraHttpHeaders = null;
    
    
    public HTTPTransport(String host, int port, String path)
    {
    	this(host, port, path, 8192);
    }
    
    public HTTPTransport(String host, int port, String path, int maxMsgSize)
    {
        this(host, port, null, 0, path, maxMsgSize);
    }

    public HTTPTransport(String host, int port, String proxyHost, int proxyPort, String path, int maxMsgSize)
    {
        this(host, port, null, 0, path, maxMsgSize, 0, 0, null);
    }
    
    public HTTPTransport(String host, int port, String proxyHost, int proxyPort, String path, int maxMsgSize, int errorOutputCount, int errorInputCount, String httpHeaders)
    {
        try
        {
            httpClient = new HttpClient();
            
            //set proxy details if necessary
            if ( (proxyHost != null) && (proxyHost.length() > 0) && (proxyPort > 0) )
                httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
            
            targetUri = new URI("http", null, host, port, path);
            
            httpClient.getHostConfiguration().setHost(targetUri);
            
            maxMessageSize = maxMsgSize;

            outputStream = new ByteArrayOutputStream(maxMessageSize);
            
            syncErrorOutputCount = errorOutputCount;
            syncErrorInputCount = errorInputCount;
            
            extraHttpHeaders = parseExtraHttpHeaders(httpHeaders);
        }
        catch (URIException e)
        {
            throw new IllegalArgumentException("invalid URI specified");
        }
    }

    public String getTargetURI()
    {
        return targetUri.toString();
    }

    public int getMaxMsgSize()
    {
        return maxMessageSize;
    }
    

    public InputStream getInputStream() 
        throws SyncException, IOException
    {
        try
        {
            //get whatever data we have buffered to send
            byte[] outputData = outputStream.toByteArray();
            if ( (outputData == null) || (outputData.length <= 0) )
                throw new IllegalStateException("no data buffered to send");

            //send it
            postRequest = new PostMethod( targetUri.toString() );
            postRequest.setRequestHeader("Content-Type", Transport.CONTENT_TYPE_WBXML);

            if (extraHttpHeaders != null)
            {
            	Enumeration keys = extraHttpHeaders.keys();
            	String key;
            	String value;
            	
            	while (keys.hasMoreElements())
            	{
            		key = (String)keys.nextElement();
            		value = (String)extraHttpHeaders.get(key);
            		if (value != null)
            			postRequest.setRequestHeader(key, value);
            	}
            }
            postRequest.setRequestBody( new ByteArrayInputStream(outputData) );
            httpClient.executeMethod(httpClient.getHostConfiguration(), postRequest);

            //generate an IO exception if required
            inputCount++;
            if (inputCount == syncErrorInputCount)
            {
                postRequest.releaseConnection();
                postRequest = null;
                throw new IOException("dummy IO exception while receiving data");
            }
            
            //check the response
            int requestStatus = postRequest.getStatusCode();
            if (requestStatus >= 400)
                throw new IllegalStateException("fatal HTTP status: " + requestStatus + " (" + postRequest.getStatusText() + ")");

            //return the response stream
            return postRequest.getResponseBodyAsStream();
        }
        catch (HttpException e)
        {
            //this exception indicate a fatal protocol error 
            throw new IllegalStateException("fatal HTTP protocol error: " + e.getMessage());
        }
    }

    public OutputStream getOutputStream() 
        throws SyncException, IOException
    {
        //generate an IO exception if required
        outputCount++;
        if (outputCount == syncErrorOutputCount)
            throw new IOException("dummy IO exception while sending data");
            
        //buffer all output until an input stream is required
        outputStream.reset();
        
        return outputStream;
    }

    public String getContentType()
    {
    	try
    	{
	        Header ctHeader = postRequest.getResponseHeader("Content-Type");
	        HeaderElement[] ctHeaderElement = ctHeader.getValues();
	        return ctHeaderElement[0].getName();
    	}
    	catch(HttpException e)
    	{
    		return null;
    	}
    }

    public void cleanup()
    {
        if (postRequest != null)
        {
            postRequest.releaseConnection();
            postRequest = null;
        }
    }
    
    private Hashtable parseExtraHttpHeaders(String httpHeaders)
    {
    	if (httpHeaders == null)
    		return null;
    	
    	Hashtable hash = null;
    	
    	StringTokenizer stt = new StringTokenizer(httpHeaders, ",");
    	String token;
    	
    	while (stt.hasMoreElements())
    	{
    		token = (String)stt.nextElement();
    		if (token != null)
    		{
    			int index = token.indexOf('=');
    			
    			if (index > 0)
    			{
    				if (hash == null)
    					hash = new Hashtable();
    				hash.put(token.substring(0, index), token.substring(index+1));
    			}
    		}
    	}
    	
    	return hash;
    }
}
