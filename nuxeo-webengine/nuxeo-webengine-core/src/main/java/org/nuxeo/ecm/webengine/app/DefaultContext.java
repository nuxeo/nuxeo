/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.ServerInjectableProviderContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultContext extends AbstractWebContext {

    // FIXME: these  members must be removed - they are redundant and buggy
    protected UriInfo info;

    protected HttpHeaders headers;

    protected ServerInjectableProviderContext sic;

    protected HttpContext hc;

    public DefaultContext(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return hc.getRequest();
    }

    @Override
    public UriInfo getUriInfo() {
        return hc.getUriInfo();
    }

    @Override
    public HttpContext getServerHttpContext() {
        return hc;
    }

    @Override
    public ServerInjectableProviderContext getServerInjectableProviderContext() {
        return sic;
    }

    public void setJerseyContext(ServerInjectableProviderContext sic, HttpContext hc) {
        this.sic = sic;
        this.hc = hc;
    }

}
