package org.nuxeo.opensocial.shindig.gadgets;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
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

public class NXMakeRequestJsonPatch extends MakeRequestHandler {

    @Inject
    public NXMakeRequestJsonPatch(ContentFetcherFactory contentFetcherFactory,
            ContentRewriterRegistry contentRewriterRegistry) {
        super(contentFetcherFactory, contentRewriterRegistry);
        this.contentFetcherFactory = contentFetcherFactory;
        this.contentRewriterRegistry = contentRewriterRegistry;
    }

    protected ContentRewriterRegistry contentRewriterRegistry;

    protected ContentFetcherFactory contentFetcherFactory;

    /**
     * Executes a request, returning the response as JSON to be handled by
     * makeRequest.
     */
    @Override
    public void fetch(HttpServletRequest request, HttpServletResponse response)
            throws GadgetException, IOException {
        HttpRequest rcr = buildHttpRequest(request);

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
            String body = "";
            if (results.getHttpStatusCode() == 200) {
                body = results.getResponseAsString();

                if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
                    body = processFeed(originalUrl, request, body);
                }
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
        if (xml.trim().equals("")) {
            // IES HACK
            return "{}";
        }
        return new FeedProcessor().process(url, xml, getSummaries, numEntries).toString();
    }
}
