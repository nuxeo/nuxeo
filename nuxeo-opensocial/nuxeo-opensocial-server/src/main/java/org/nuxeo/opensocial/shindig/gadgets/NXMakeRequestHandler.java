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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This is a way to pass our session through the make request handler mechanism.
 * We can't use cookies directly because shindig will strip them.
 */
@Singleton
public class NXMakeRequestHandler extends MakeRequestHandler {

    protected OpenSocialService svc;

    private static final Log log = LogFactory.getLog(NXMakeRequestHandler.class);

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

    @Override
    protected HttpRequest buildHttpRequest(HttpServletRequest request)
            throws GadgetException {
        HttpRequest req = super.buildHttpRequest(request);
        String auth = req.getUri().getAuthority();
        if (auth != null) {
            if (auth.indexOf(':') != -1) {
                auth = auth.substring(0, auth.indexOf(':')); // foo:8080
            }
            for (String host : svc.getTrustedHosts()) {
                if (host.trim().equalsIgnoreCase(auth.trim())) {
                    if (request.isRequestedSessionIdValid()) {
                        if (request.isRequestedSessionIdFromCookie()) {
                            req.addHeader("Cookie", "JSESSIONID="
                                    + request.getRequestedSessionId());
                        }
                    }
                    break;
                }
            }
        }
        return req;
    }
}