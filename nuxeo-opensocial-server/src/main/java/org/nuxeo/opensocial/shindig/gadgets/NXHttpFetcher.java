/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.shindig.gadgets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.internal.Preconditions;
import com.google.inject.name.Named;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.nuxeo.opensocial.helper.ProxyHelper;

/**
 * We have to copy BasicHttpFetcher because we must override the way proxy is
 * used (it's not handling authentication), and since the makeRespons method is
 * private, we cant' use it. Therefore, as there is only two methods in the base
 * class : the one we want to override and the one that is private, it makes non
 * sense to find a way to override it.
 *
 * @author dmetzler
 *
 */
public class NXHttpFetcher implements HttpFetcher {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    private static final int DEFAULT_MAX_OBJECT_SIZE = 0; // no limit

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static final String SHINDIG_PROXY_PORT = "shindig.proxy.proxyPort";

    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";

    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";

    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    // mutable fields must be volatile
    private volatile int maxObjSize;

    private volatile int connectionTimeoutMs;

    /**
     * Creates a new fetcher for fetching HTTP objects. Not really suitable for
     * production use. Use of an HTTP proxy for security is also necessary for
     * production deployment.
     *
     * @param maxObjSize Maximum size, in bytes, of the object we will fetch, 0
     *            if no limit..
     * @param connectionTimeoutMs timeout, in milliseconds, for requests.
     */
    public NXHttpFetcher(int maxObjSize, int connectionTimeoutMs) {
        setMaxObjectSizeBytes(maxObjSize);
        setConnectionTimeoutMs(connectionTimeoutMs);
    }

    /**
     * Creates a new fetcher using the default maximum object size and timeout
     * -- no limit and 5 seconds.
     */
    public NXHttpFetcher() {
        this(DEFAULT_MAX_OBJECT_SIZE, DEFAULT_CONNECT_TIMEOUT_MS);
    }

    /**
     * Change the global maximum fetch size (in bytes) for all fetches.
     *
     * @param maxObjectSizeBytes value for maximum number of bytes, or 0 for no
     *            limit
     */
    @Inject(optional = true)
    public void setMaxObjectSizeBytes(
            @Named("shindig.http.client.max-object-size-bytes") int maxObjectSizeBytes) {
        this.maxObjSize = maxObjectSizeBytes;
    }

    /**
     * Change the global connection timeout for all fetchs.
     *
     * @param connectionTimeoutMs new connection timeout in milliseconds
     */
    @Inject(optional = true)
    public void setConnectionTimeoutMs(
            @Named("shindig.http.client.connection-timeout-ms") int connectionTimeoutMs) {
        Preconditions.checkArgument(connectionTimeoutMs > 0,
                "connection-timeout-ms must be greater than 0");
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    /**
     * @param httpMethod
     * @param responseCode
     * @return A HttpResponse object made by consuming the response of the given
     *         HttpMethod.
     * @throws java.io.IOException
     */
    private HttpResponse makeResponse(HttpMethod httpMethod, int responseCode)
            throws IOException {
        Map<String, String> headers = Maps.newHashMap();

        if (httpMethod.getResponseHeaders() != null) {
            for (Header h : httpMethod.getResponseHeaders()) {
                headers.put(h.getName(), h.getValue());
            }
        }

        // The first header is always null here to provide the response body.
        headers.remove(null);

        // Find the response stream - the error stream may be valid in cases
        // where the input stream is not.
        InputStream responseBodyStream = null;
        try {
            responseBodyStream = httpMethod.getResponseBodyAsStream();
        } catch (IOException e) {
            // normal for 401, 403 and 404 responses, for example...
        }

        if (responseBodyStream == null) {
            // Fall back to zero length response.
            responseBodyStream = new ByteArrayInputStream(
                    ArrayUtils.EMPTY_BYTE_ARRAY);
        }

        String encoding = headers.get("Content-Encoding");

        // Create the appropriate stream wrapper based on the encoding type.
        InputStream is = responseBodyStream;
        if (encoding == null) {
            is = responseBodyStream;
        } else if (encoding.equalsIgnoreCase("gzip")) {
            is = new GZIPInputStream(responseBodyStream);
        } else if (encoding.equalsIgnoreCase("deflate")) {
            Inflater inflater = new Inflater(true);
            is = new InflaterInputStream(responseBodyStream, inflater);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int totalBytesRead = 0;
        int currentBytesRead;

        while ((currentBytesRead = is.read(buffer)) != -1) {
            output.write(buffer, 0, currentBytesRead);
            totalBytesRead += currentBytesRead;

            if (maxObjSize > 0 && totalBytesRead > maxObjSize) {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(output);
                // Exceeded max # of bytes
                return HttpResponse.badrequest("Exceeded maximum number of bytes - "
                        + this.maxObjSize);
            }
        }

        return new HttpResponseBuilder().setHttpStatusCode(responseCode).setResponse(
                output.toByteArray()).addHeaders(headers).create();
    }

    /** {@inheritDoc} */
    public HttpResponse fetch(HttpRequest request) {
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod;
        String methodType = request.getMethod();
        String requestUri = request.getUri().toString();

        ProxyHelper.fillProxy(httpClient, requestUri);

        // true for non-HEAD requests
        boolean requestCompressedContent = true;

        if ("POST".equals(methodType) || "PUT".equals(methodType)) {
            EntityEnclosingMethod enclosingMethod = ("POST".equals(methodType)) ? new PostMethod(
                    requestUri) : new PutMethod(requestUri);

            if (request.getPostBodyLength() > 0) {
                enclosingMethod.setRequestEntity(new InputStreamRequestEntity(
                        request.getPostBody()));
                enclosingMethod.setRequestHeader("Content-Length",
                        String.valueOf(request.getPostBodyLength()));
            }
            httpMethod = enclosingMethod;
        } else if ("DELETE".equals(methodType)) {
            httpMethod = new DeleteMethod(requestUri);
        } else if ("HEAD".equals(methodType)) {
            httpMethod = new HeadMethod(requestUri);
        } else {
            httpMethod = new GetMethod(requestUri);
        }

        httpMethod.setFollowRedirects(false);
        httpMethod.getParams().setSoTimeout(connectionTimeoutMs);

        if (requestCompressedContent)
            httpMethod.setRequestHeader("Accept-Encoding", "gzip, deflate");

        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            httpMethod.setRequestHeader(entry.getKey(),
                    StringUtils.join(entry.getValue(), ','));
        }

        try {

            int statusCode = httpClient.executeMethod(httpMethod);

            // Handle redirects manually
            if (request.getFollowRedirects()
                    && ((statusCode == HttpStatus.SC_MOVED_TEMPORARILY)
                            || (statusCode == HttpStatus.SC_MOVED_PERMANENTLY)
                            || (statusCode == HttpStatus.SC_SEE_OTHER) || (statusCode == HttpStatus.SC_TEMPORARY_REDIRECT))) {

                Header header = httpMethod.getResponseHeader("location");
                if (header != null) {
                    String redirectUri = header.getValue();

                    if ((redirectUri == null) || (redirectUri.equals(""))) {
                        redirectUri = "/";
                    }
                    httpMethod.releaseConnection();
                    httpMethod = new GetMethod(redirectUri);

                    statusCode = httpClient.executeMethod(httpMethod);
                }
            }

            return makeResponse(httpMethod, statusCode);

        } catch (IOException e) {
            if (e instanceof java.net.SocketTimeoutException
                    || e instanceof java.net.SocketException) {
                return HttpResponse.timeout();
            }

            return HttpResponse.error();

        } finally {
            httpMethod.releaseConnection();
        }
    }
}
