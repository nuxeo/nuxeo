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
 */
package org.nuxeo.ecm.webengine.model.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.ErrorHandler;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultErrorHandler implements ErrorHandler {

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.model.ErrorHandler#handleError(javax.ws.rs.WebApplicationException)
     */
    public Object handleError(WebContext ctx, WebApplicationException e) {
        Resource rs = ctx.tail();
        if (rs != null) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(rs.getTemplate("error/error_401.ftl")).build();
          } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(rs.getTemplate("error/error_404.ftl")).build();
          }
        }
        return WebException.handleError(e);
    }
    
}
