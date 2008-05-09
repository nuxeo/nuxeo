/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rendering;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.SimpleContextView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServletRequestView extends SimpleContextView {

    private static final long serialVersionUID = 0L;

    protected static final String[] KEYS = new String[] {
       "HTTP_REQUEST_URI",
       "HTTP_REQUEST_URL" //TODO
    };

    protected static final HttpServletRequest getRequest(RenderingContext ctx) {
        return ((ServletRenderingContext) ctx).getRequest();
    }

    protected static final HttpServletResponse getResponse(RenderingContext ctx) {
        return ((ServletRenderingContext) ctx).getResponse();
    }

    @Override
    public Object get(String key, RenderingContext ctx) {
        Object val = get(key);
        if (val == null) {
            if ("user".equals(key)) {
                Principal principal = getRequest(ctx).getUserPrincipal();
                return principal == null ? "anonymous" : principal.getName();
            } else if ("context".equals(key)) {
                return ((ServletRenderingContext)ctx).getWebContext();
            } else if ("request".equals(key)) {
                return getRequest(ctx);
            } else if ("response".equals(key)) {
                return getResponse(ctx);
            } else if ("requestUri".equals(key)) {
                return getRequest(ctx).getRequestURI();
            } else if ("basePath".equals(key)) {
                HttpServletRequest req = getRequest(ctx);
                return req.getContextPath() + req.getServletPath();
//            } else if ("http".equals(key)) {
//                return getHttpInfo(key);
            }
        }
        return UNKNOWN;
    }

    public Collection<String> keys() {
        Collection<String> keys = keySet();
        keys.addAll(Arrays.asList(KEYS));
        return keys;
    }

//    public Map<String,String> getHttpInfo(HttpServletRequest req, String key) {
//        if ("HTTP_REQUEST_URI".equals(key)) {
//            return req.getRequestURI();
//        } else if ("HTTP_REQUEST_URL".equals(key)) {
//            return req.getRequestURL();
//        } else if ("HTTP_SESSION_ID".equals(key)) {
//            return req.getRequestedSessionId();
//        } else if ("HTTP_PATH_INFO".equals(key)) {
//            return req.getPathInfo();
//        } else if ("HTTP_PATH_TRANSLATED".equals(key)) {
//            return req.getPathTranslated();
//        } else if ("HTTP_QUERY_STRING".equals(key)) {
//            return req.getQueryString();
//        } else if ("HTTP_METHOD".equals(key)) {
//            return req.getMethod();
//        } else if ("HTTP_REMOTE_ADDRESS".equals(key)) {
//            return req.getRemoteAddr();
//        } else if ("HTTP_REMOTE_HOST".equals(key)) {
//            return req.getRemoteHost();
//        } else if ("HTTP_REMOTE_USER".equals(key)) {
//            return req.getRemoteUser();
//        } else if ("HTTP_REMOTE_PORT".equals(key)) {
//            return req.getRemotePort();
//        } else if ("HTTP_CONTEXT_PATH".equals(key)) {
//            return req.getContextPath();
//        } else if ("HTTP_SERVLET_PATH".equals(key)) {
//            return req.getServletPath();
//        } else if ("HTTP_SERVER_NAME".equals(key)) {
//            return req.getServerName();
//        } else if ("HTTP_AUTH_TYPE".equals(key)) {
//            return req.getAuthType();
//        }
//    }

}
