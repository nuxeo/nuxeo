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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultContext extends AbstractWebContext {

    // FIXME: these two members must be removed - they are redundant and buggy
    protected UriInfo info;

    protected HttpHeaders headers;

    public DefaultContext(HttpServletRequest request) {
        super(request);
    }

    @Override
    @Deprecated
    public HttpHeaders getHttpHeaders() {
        // throw new UnsupportedOperationException("Deprecated. Use @Context HttpHeaders to inject this object");
        return headers;
    }

    @Override
    @Deprecated
    public UriInfo getUriInfo() {
        // throw new UnsupportedOperationException("Deprecated. Use @Context UriInfo to inject this object");
        return info;
    }

    public void setUriInfo(UriInfo info) {
        this.info = info;
    }

    public void setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

}
