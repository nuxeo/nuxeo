package org.nuxeo.opensocial.shindig.gadgets;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.http.HttpCache;
import org.apache.shindig.gadgets.http.HttpCacheKey;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProxySelectorHttpFetcher implements HttpFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_MAX_OBJECT_SIZE = 1024 * 1024;
    private final HttpCache cache;

    @Inject
    ProxySelector proxySelector;

    public ProxySelectorHttpFetcher(HttpCache cache, int maxObjSize) {
        this.cache = cache;
      }

    @Inject
    public ProxySelectorHttpFetcher(HttpCache cache) {
      this(cache, DEFAULT_MAX_OBJECT_SIZE);
    }


    /**
     * Initializes the connection.
     *
     * @param request
     * @return The opened connection
     * @throws IOException
     */
    private HttpURLConnection getConnection(HttpRequest request) throws IOException {
      URL url = new URL(request.getUri().toString());


      HttpURLConnection fetcher = null;

      List<Proxy> proxies = proxySelector.select(request.getUri().toJavaUri());
      if(proxies.size() > 0 ) {
          fetcher = (HttpURLConnection) url.openConnection(proxies.get(0));
      } else {
          fetcher = (HttpURLConnection) url.openConnection();
      }

      fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
      fetcher.setRequestProperty("Accept-Encoding", "gzip, deflate");
      fetcher.setInstanceFollowRedirects(request.getFollowRedirects());
      for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
        fetcher.setRequestProperty(entry.getKey(), StringUtils.join(entry.getValue(), ','));
      }
      fetcher.setDefaultUseCaches(!request.getIgnoreCache());
      return fetcher;
    }

    /**
     * @param fetcher
     * @return A HttpResponse object made by consuming the response of the
     *     given HttpURLConnection.
     */
    private HttpResponse makeResponse(HttpURLConnection fetcher) throws IOException {
      Map<String, List<String>> headers = Maps.newHashMap(fetcher.getHeaderFields());
      // The first header is always null here to provide the response body.
      headers.remove(null);
      int responseCode = fetcher.getResponseCode();
      // Find the response stream - the error stream may be valid in cases
      // where the input stream is not.
      InputStream baseIs = null;
      try {
        baseIs = fetcher.getInputStream();
      } catch (IOException e) {
        // normal for 401, 403 and 404 responses, for example...
      }
      if (baseIs == null) {
        // Try for an error input stream
        baseIs = fetcher.getErrorStream();
      }
      if (baseIs == null) {
        // Fall back to zero length response.
        baseIs = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY);
      }

      String encoding = fetcher.getContentEncoding();
      // Create the appropriate stream wrapper based on the encoding type.
      InputStream is = null;
      if (encoding == null) {
        is = baseIs;
      } else if (encoding.equalsIgnoreCase("gzip")) {
        is = new GZIPInputStream(baseIs);
      } else if (encoding.equalsIgnoreCase("deflate")) {
        Inflater inflater = new Inflater(true);
        is = new InflaterInputStream(baseIs, inflater);
      }

      byte[] body = IOUtils.toByteArray(is);
      return new HttpResponseBuilder()
          .setHttpStatusCode(responseCode)
          .setResponse(body)
          .addAllHeaders(headers)
          .create();
    }


    public HttpResponse fetch(HttpRequest request) {
        HttpCacheKey cacheKey = new HttpCacheKey(request);
        HttpResponse response = cache.getResponse(cacheKey, request);
        if (response != null) {
          return response;
        }
        try {
          HttpURLConnection fetcher = getConnection(request);
          fetcher.setRequestMethod(request.getMethod());
          if (!"GET".equals(request.getMethod())) {
            fetcher.setUseCaches(false);
          }
          fetcher.setRequestProperty("Content-Length",
              String.valueOf(request.getPostBodyLength()));
          if (request.getPostBodyLength() > 0) {
            fetcher.setDoOutput(true);
            IOUtils.copy(request.getPostBody(), fetcher.getOutputStream());
          }
          response = makeResponse(fetcher);
          return cache.addResponse(cacheKey, request, response);
        } catch (IOException e) {
          if (e instanceof java.net.SocketTimeoutException ||
              e instanceof java.net.SocketException) {
            return HttpResponse.timeout();
          }
          return HttpResponse.error();
        }
      }

}
