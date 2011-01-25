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

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;
import org.apache.shindig.gadgets.servlet.ProxyBase;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

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

    private static final Log log = LogFactory.getLog(NXMakeRequestHandler.class);

    public static final String NUXEO_REST = "/nuxeo/restAPI";

    public static final String NUXEO_WEBENG = "/nuxeo/site";

    public static final String COOKIE = "Cookie";

    public static final String JSESSIONCOOKIE = "JSESSIONID=";

    protected OpenSocialService svc;

    @Inject
    public NXMakeRequestHandler(RequestPipeline requestPipeline,
            RequestRewriterRegistry contentRewriterRegistry) {
        super(requestPipeline, contentRewriterRegistry);
        try {
            svc = Framework.getService(OpenSocialService.class);
        } catch (Exception e) {
            log.error("Unable to find opensocial service!", e);
        }

    }

    /**
     * Format a response as JSON, including additional JSON inserted by chained
     * content fetchers.
     */
    @Override
    protected String convertResponseToJson(SecurityToken authToken,
            HttpServletRequest request, HttpResponse results)
            throws GadgetException {
        String originalUrl = request.getParameter(ProxyBase.URL_PARAM);
        String body = results.getResponseAsString();
        if (body.length() > 0) {
            if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
                body = NXprocessFeed(originalUrl, request, body);
            }
        }
        Map<String, Object> resp = FetchResponseUtils.getResponseAsJson(
                results, null, body);

        if (authToken != null) {
            String updatedAuthToken = authToken.getUpdatedToken();
            if (updatedAuthToken != null) {
                resp.put("st", updatedAuthToken);
            }
        }

        // Use raw param as key as URL may have to be decoded
        return JsonSerializer.serialize(Collections.singletonMap(originalUrl,
                resp));
    }

    /**
     * Processes a feed (RSS or Atom) using FeedProcessor.
     */
    private String NXprocessFeed(String url, HttpServletRequest req, String xml)
            throws GadgetException {
        boolean getSummaries = Boolean.parseBoolean(getParameter(req,
                GET_SUMMARIES_PARAM, "false"));
        int numEntries = Integer.parseInt(getParameter(req, NUM_ENTRIES_PARAM,
                DEFAULT_NUM_ENTRIES));
        return new NXFeedProcessor().process(url, xml, getSummaries, numEntries).toString();
    }

    @Override
    protected HttpRequest buildHttpRequest(HttpServletRequest request)
            throws GadgetException {
        HttpRequest req = super.buildHttpRequest(request);

        if (!svc.propagateJSESSIONIDToTrustedHosts()) {
            return req;
        }

        String auth = req.getUri().getAuthority();
        boolean done = false;
        if (auth != null) {
            if (auth.indexOf(':') != -1) {
                auth = auth.substring(0, auth.indexOf(':')); // foo:8080
            }
            for (String host : svc.getTrustedHosts()) {
                if (host.trim().equalsIgnoreCase(auth.trim())) {
                    if (request.isRequestedSessionIdValid()) {
                        if (request.isRequestedSessionIdFromCookie()) {
                            req.addHeader(COOKIE, JSESSIONCOOKIE
                                    + request.getRequestedSessionId());
                            done = true;
                        }
                    }
                    break;
                }
            }
            if (!done) {
                String path = req.getUri().getPath();
                if ((path.startsWith(NUXEO_REST))
                        || (path.startsWith(NUXEO_WEBENG))) {
                    req.addHeader(COOKIE, JSESSIONCOOKIE
                            + request.getRequestedSessionId());
                }
            }
        }
        return req;
    }

}
