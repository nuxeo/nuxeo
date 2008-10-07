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

package org.nuxeo.ecm.webengine.server.resteasy;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.interception.ResourceMethodContext;
import org.jboss.resteasy.core.interception.ResourceMethodInterceptor;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.ViewDescriptor;
import org.nuxeo.ecm.webengine.model.WebView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SecurityInterceptor implements ResourceMethodInterceptor {

    public boolean accepted(ResourceMethod method) {
        //Guard guard = method.getMethod().getAnnotation(Guard.class);
        WebView view = method.getMethod().getAnnotation(WebView.class);
        return view != null;
    }

    public Response invoke(ResourceMethodContext ctx) throws Failure,
    ApplicationException, WebApplicationException {
        Object target = ctx.getTarget();
        if (target instanceof Resource) {
            WebView wv = ctx.getMethod().getMethod().getAnnotation(WebView.class);
            if (wv != null) {                
                Resource rs = (Resource)target;
                ViewDescriptor vd = rs.getView(wv.name());
                if (vd != null) { 
                    if (!vd.isEnabled(rs)) {
                        throw new WebSecurityException(wv.name());                    
                    } 
                    if (vd.getAuto()) {
                        Template tpl = vd.getTemplate(rs);
                        if (!tpl.isResolved()) {
                            // get media type information
                            MediaType[] produces = ctx.getMethod().getProduces();
                            if (produces != null && produces.length > 0) {
                                tpl.mediaType(produces[0]);
                            }
                            tpl.resolve(); 
                        }
                        return Response.ok().entity(tpl).build(); 
                    }
                }
            }
        }
        return ctx.proceed();
    }

}
