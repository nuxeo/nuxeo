/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig.gadgets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author cusgu
 *
 *         Patch FeedProcessor in order to retrieve extra elements from RSS 2.0
 *         feeds : - enclosure
 *
 */
@Singleton
public class NXMakeRequestHandler extends MakeRequestHandler {

    private static final String HTTP_PROXY_HOST = "http.proxyHost";

    private final ContentFetcherFactory contentFetcherFactory;

    private final ContentRewriterRegistry contentRewriterRegistry;

    private static final String HTTP_PROXY_SET = "http.proxySet";

    private static final String HTTP_PROXY_PORT = "http.proxyPort";

    private static final String SHINDIG_PROXY_PROXY_PORT = "shindig.proxy.proxyPort";

    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";

    private static final String SHINDIG_PROXY_PROXY_SET = "shindig.proxy.proxySet";

    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";

    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    private static final Log LOG = LogFactory.getLog(NXMakeRequestHandler.class);

    public static String AUTH_SESSION_HEADER = "X-NUXEO-INTEGRATED-AUTH";

    @Inject
    public NXMakeRequestHandler(ContentFetcherFactory contentFetcherFactory,
            ContentRewriterRegistry contentRewriterRegistry) {
        super(contentFetcherFactory, contentRewriterRegistry);
        this.contentFetcherFactory = contentFetcherFactory;
        this.contentRewriterRegistry = contentRewriterRegistry;
    }

    /**
     * Executes a request, returning the response as JSON to be handled by
     * makeRequest.
     */
    @Override
    public void fetch(HttpServletRequest request, HttpServletResponse response)
            throws GadgetException, IOException {
        HttpRequest rcr = buildHttpRequest(request);
        /*
         * LOG.info("NXMakeRequestHandler - fetch method - shindig set "+Framework
         * .getProperty(SHINDIG_PROXY_PROXY_SET));
         * LOG.info("NXMakeRequestHandler - shindig host"
         * +Framework.getProperty(SHINDIG_PROXY_PROXY_HOST));
         * LOG.info("NXMakeRequestHandler - shindig port"
         * +Framework.getProperty(SHINDIG_PROXY_PROXY_PORT));
         *
         * System.setProperty(HTTP_PROXY_SET,
         * Framework.getProperty(SHINDIG_PROXY_PROXY_SET));
         * System.setProperty(HTTP_PROXY_HOST,
         * Framework.getProperty(SHINDIG_PROXY_PROXY_HOST));
         * System.setProperty(HTTP_PROXY_PORT,
         * Framework.getProperty(SHINDIG_PROXY_PROXY_PORT));
         *
         * if (!(Framework.getProperty(SHINDIG_PROXY_USER) == null ||
         * Framework.getProperty(SHINDIG_PROXY_PASSWORD) == null)) {
         *
         * LOG.info("NXMakeRequestHandler - Authenticator "+
         * Framework.getProperty(SHINDIG_PROXY_USER));
         * LOG.info("NXMakeRequestHandler - Authenticator "+
         * Framework.getProperty(SHINDIG_PROXY_PASSWORD));
         * Authenticator.setDefault(new Authenticator() {
         *
         * @Override protected PasswordAuthentication
         * getPasswordAuthentication() {
         *
         * return new PasswordAuthentication(
         * Framework.getProperty(SHINDIG_PROXY_USER), Framework.getProperty(
         * SHINDIG_PROXY_PASSWORD) .toCharArray()); } }); }
         */

        // propagate Nuxeo SessionId
        String sessionId = rcr.getHeader(AUTH_SESSION_HEADER);
        if (sessionId!=null) {
            rcr.addHeader("Cookie", "JSESSIONID=" + sessionId);
        }

        // Serialize the response
        HttpResponse results = contentFetcherFactory.fetch(rcr);

        // Rewrite the response
        if (contentRewriterRegistry != null) {
            results = contentRewriterRegistry.rewriteHttpResponse(rcr, results);
        }

        // Serialize the response
        String output = convertResponseToJson(rcr.getSecurityToken(), request,
                results);

        // Find and set the refresh interval
        setResponseHeaders(request, response, results);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNPARSEABLE_CRUFT + output);
    }

    /**
     * Generate a remote content request based on the parameters sent from the
     * client.
     *
     * @throws GadgetException
     */
    private HttpRequest buildHttpRequest(HttpServletRequest request)
            throws GadgetException {
        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }

        Uri url = validateUrl(request.getParameter(URL_PARAM));

        HttpRequest req = new HttpRequest(url).setMethod(
                getParameter(request, METHOD_PARAM, "GET")).setPostBody(
                getParameter(request, POST_DATA_PARAM, "").getBytes()).setContainer(
                getContainer(request));

        String headerData = getParameter(request, HEADERS_PARAM, "");
        if (headerData.length() > 0) {
            String[] headerList = headerData.split("&");
            for (String header : headerList) {
                String[] parts = header.split("=");
                if (parts.length != 2) {
                    throw new GadgetException(
                            GadgetException.Code.INTERNAL_SERVER_ERROR,
                            "Malformed header specified,");
                }
                req.addHeader(Utf8UrlCoder.decode(parts[0]),
                        Utf8UrlCoder.decode(parts[1]));
            }
        }

        removeUnsafeHeaders(req);

        req.setIgnoreCache("1".equals(request.getParameter(NOCACHE_PARAM)));

        if (request.getParameter(GADGET_PARAM) != null) {
            req.setGadget(Uri.parse(request.getParameter(GADGET_PARAM)));
        }

        // Allow the rewriter to use an externally forced mime type. This is
        // needed
        // allows proper rewriting of <script src="x"/> where x is returned with
        // a content type like text/html which unfortunately happens all too
        // often
        req.setRewriteMimeType(request.getParameter(REWRITE_MIME_TYPE_PARAM));

        // Figure out whether authentication is required
        AuthType auth = AuthType.parse(getParameter(request, AUTHZ_PARAM, null));
        req.setAuthType(auth);
        if (auth != AuthType.NONE) {
            req.setSecurityToken(extractAndValidateToken(request));
            req.setOAuthArguments(new OAuthArguments(auth, request));
        }
        return req;
    }

    /**
     * Removes unsafe headers from the header set.
     */
    private void removeUnsafeHeaders(HttpRequest request) {
        // Host must be removed.
        final String[] badHeaders = new String[] {
        // No legitimate reason to over ride these.
                // TODO: We probably need to test variations as well.
                "Host", "Accept", "Accept-Encoding" };
        for (String bad : badHeaders) {
            request.removeHeader(bad);
        }
    }

    /**
     * Format a response as JSON, including additional JSON inserted by chained
     * content fetchers.
     */
    private String convertResponseToJson(SecurityToken authToken,
            HttpServletRequest request, HttpResponse results)
            throws GadgetException {
        try {
            String originalUrl = request.getParameter(ProxyBase.URL_PARAM);
            String body = results.getResponseAsString();
            if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
                body = processFeed(originalUrl, request, body);
            }
            JSONObject resp = FetchResponseUtils.getResponseAsJson(results,
                    body);

            if (authToken != null) {
                String updatedAuthToken = authToken.getUpdatedToken();
                if (updatedAuthToken != null) {
                    resp.put("st", updatedAuthToken);
                }
            }
            // Use raw param as key as URL may have to be decoded
            return new JSONObject().put(originalUrl, resp).toString();
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * @param request
     * @return A valid token for the given input.
     */
    private SecurityToken extractAndValidateToken(HttpServletRequest request)
            throws GadgetException {
        SecurityToken token = new AuthInfo(request).getSecurityToken();
        if (token == null) {
            throw new GadgetException(
                    GadgetException.Code.INVALID_SECURITY_TOKEN);
        }
        return token;
    }

    /**
     * Processes a feed (RSS or Atom) using FeedProcessor.
     */
    private String processFeed(String url, HttpServletRequest req, String xml)
            throws GadgetException {
        boolean getSummaries = Boolean.parseBoolean(getParameter(req,
                GET_SUMMARIES_PARAM, "false"));
        int numEntries = Integer.parseInt(getParameter(req, NUM_ENTRIES_PARAM,
                DEFAULT_NUM_ENTRIES));
        return new NXFeedProcessor().process(url, xml, getSummaries, numEntries).toString();
    }

}
