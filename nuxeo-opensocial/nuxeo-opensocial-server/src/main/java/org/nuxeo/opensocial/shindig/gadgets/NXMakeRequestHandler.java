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

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.RequestRewriterRegistry;
import org.apache.shindig.gadgets.servlet.MakeRequestHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This is a way to pass our session through the make request handler mechanism.
 * We can't use cookies directly because shindig will strip them.
 */
@Singleton
public class NXMakeRequestHandler extends MakeRequestHandler {

    @Inject
    public NXMakeRequestHandler(RequestPipeline requestPipeline,
            RequestRewriterRegistry contentRewriterRegistry) {
        super(requestPipeline, contentRewriterRegistry);
    }

    @Override
    protected HttpRequest buildHttpRequest(HttpServletRequest request)
            throws GadgetException {
        HttpRequest req = super.buildHttpRequest(request);
        if (request.isRequestedSessionIdValid()) {
            if (request.isRequestedSessionIdFromCookie()) {
                req.addHeader("Cookie", "JSESSIONID="
                        + request.getRequestedSessionId());
            }
        }
        // if (req.getHeader("X-NUXEO-INTEGRATED-AUTH") != null) {
        // req.addHeader("Cookie", "JSESSIONID="
        // + req.getHeader("X-NUXEO-INTEGRATED-AUTH"));
        // }
        return req;
    }
}