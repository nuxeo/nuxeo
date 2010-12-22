package org.apache.shindig.gadgets.servlet;

import static org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.PARAM_NO_EXPAND;
import static org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.PARAM_RESIZE_HEIGHT;
import static org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.PARAM_RESIZE_QUALITY;
import static org.apache.shindig.gadgets.rewrite.image.BasicImageRewriter.PARAM_RESIZE_WIDTH;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.servlet.ProxyHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NXProxyHandler extends ProxyBase {
    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    private static final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

    private static final String[] INTEGER_RESIZE_PARAMS = new String[] {
            PARAM_RESIZE_HEIGHT, PARAM_RESIZE_WIDTH, PARAM_RESIZE_QUALITY,
            PARAM_NO_EXPAND };

    static final String FALLBACK_URL_PARAM = "fallback_url";

    private final RequestPipeline requestPipeline;
    private final LockedDomainService lockedDomainService;
    private final RequestRewriterRegistry contentRewriterRegistry;

    @Inject
    public NXProxyHandler(RequestPipeline requestPipeline,
            LockedDomainService lockedDomainService,
            RequestRewriterRegistry contentRewriterRegistry) {
        this.requestPipeline = requestPipeline;
        this.lockedDomainService = lockedDomainService;
        this.contentRewriterRegistry = contentRewriterRegistry;
    }

    /**
     * Generate a remote content request based on the parameters sent from the
     * client.
     */
    private HttpRequest nxbuildHttpRequest(HttpServletRequest request,
            String urlParam) throws GadgetException {
        String theUrl = request.getParameter(urlParam);
        if (theUrl == null) {
            return null;
        }
        Uri url = validateUrl(theUrl);

        HttpRequest req = new HttpRequest(url).setContainer(getContainer(request));

        copySanitizedIntegerParams(request, req);

        if (request.getParameter(GADGET_PARAM) != null) {
            req.setGadget(Uri.parse(request.getParameter(GADGET_PARAM)));
        }


        // Forwared Accept-Language header in proxy request since in some case (Google jsapi for
        // instance), some l10n stuffs are put in js files.
        String acceptLanguage = request.getHeader(ACCEPT_LANGUAGE);
        if (acceptLanguage != null) {
            req.setHeader(ACCEPT_LANGUAGE, acceptLanguage);
        }

        // Allow the rewriter to use an externally forced MIME type. This is
        // needed
        // allows proper rewriting of <script src="x"/> where x is returned with
        // a content type like text/html which unfortunately happens all too
        // often
        req.setRewriteMimeType(request.getParameter(REWRITE_MIME_TYPE_PARAM));

        req.setIgnoreCache(getIgnoreCache(request));

        req.setSanitizationRequested("1".equals(request.getParameter(SANITIZE_CONTENT_PARAM)));

        // If the proxy request specifies a refresh param then we want to force
        // the min TTL for
        // the retrieved entry in the cache regardless of the headers on the
        // content when it
        // is fetched from the original source.
        if (request.getParameter(REFRESH_PARAM) != null) {
            try {
                req.setCacheTtl(Integer.parseInt(request.getParameter(REFRESH_PARAM)));
            } catch (NumberFormatException nfe) {
                // Ignore
            }
        }
        this.setRequestHeaders(request, req);
        return req;
    }

    private void copySanitizedIntegerParams(HttpServletRequest request,
            HttpRequest req) {
        for (String resizeParamName : INTEGER_RESIZE_PARAMS) {
            if (request.getParameter(resizeParamName) != null) {
                req.setParam(
                        resizeParamName,
                        NumberUtils.createInteger(request.getParameter(resizeParamName)));
            }
        }
    }

    @Override
    protected void doFetch(HttpServletRequest request,
            HttpServletResponse response) throws IOException, GadgetException {
        if (request.getHeader("If-Modified-Since") != null) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        String host = request.getHeader("Host");
        if (!lockedDomainService.isSafeForOpenProxy(host)) {
            // Force embedded images and the like to their own domain to avoid
            // XSS
            // in gadget domains.
            String msg = "Embed request for url "
                    + getParameter(request, URL_PARAM, "")
                    + " made to wrong domain " + host;
            logger.info(msg);
            throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
                    msg);
        }

        HttpRequest rcr = nxbuildHttpRequest(request, URL_PARAM);
        HttpResponse results = requestPipeline.execute(rcr);

        if (results.isError()) {
            // Error: try the fallback. Particularly useful for proxied images.
            HttpRequest fallbackRcr = nxbuildHttpRequest(request,
                    FALLBACK_URL_PARAM);
            if (fallbackRcr != null) {
                results = requestPipeline.execute(fallbackRcr);
            }
        }

        if (contentRewriterRegistry != null) {
            try {
                results = contentRewriterRegistry.rewriteHttpResponse(rcr,
                        results);
            } catch (RewritingException e) {
                throw new GadgetException(
                        GadgetException.Code.INTERNAL_SERVER_ERROR, e);
            }
        }

        for (Map.Entry<String, String> entry : results.getHeaders()
                .entries()) {
            String name = entry.getKey();
            if (!DISALLOWED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                response.addHeader(name, entry.getValue());
            }
        }

        String responseType = results.getHeader("Content-Type");
        if (!StringUtils.isEmpty(rcr.getRewriteMimeType())) {
            String requiredType = rcr.getRewriteMimeType();
            // Use a 'Vary' style check on the response
            if (requiredType.endsWith("/*")
                    && !StringUtils.isEmpty(responseType)) {
                requiredType = requiredType.substring(0,
                        requiredType.length() - 2);
                if (!responseType.toLowerCase()
                        .startsWith(requiredType.toLowerCase())) {
                    response.setContentType(requiredType);
                    responseType = requiredType;
                }
            } else {
                response.setContentType(requiredType);
                responseType = requiredType;
            }
        }

        setResponseHeaders(request, response, results);

        if (results.getHttpStatusCode() != HttpResponse.SC_OK) {
            response.sendError(results.getHttpStatusCode());
        }

        IOUtils.copy(results.getResponse(), response.getOutputStream());
    }
}
