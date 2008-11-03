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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.interception.ResourceMethodContext;
import org.jboss.resteasy.core.interception.ResourceMethodInterceptor;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SecurityInterceptor implements ResourceMethodInterceptor {

    protected volatile Map<Method, Guard> cache = new ConcurrentHashMap<Method, Guard>();

    public void flushCache() {
        cache = new ConcurrentHashMap<Method, Guard>();
    }

    public boolean accepted(ResourceMethod method) {
        return null != method.getMethod().getAnnotation(org.nuxeo.ecm.webengine.model.Guard.class);
    }

    public Response invoke(ResourceMethodContext ctx) throws Failure,
    ApplicationException, WebApplicationException {
        Object target = ctx.getTarget();
        if (target instanceof Resource) {
            Method m = ctx.getMethod().getMethod();
            org.nuxeo.ecm.webengine.model.Guard ganno = m.getAnnotation(org.nuxeo.ecm.webengine.model.Guard.class);
            if (ganno != null) {
                checkAccess(ctx, ganno, m, (Resource)target);
            }
        }
        return ctx.proceed();
    }

    protected void checkAccess(ResourceMethodContext ctx, org.nuxeo.ecm.webengine.model.Guard ganno, Method m, Resource rs) {
        Guard guard = null;
        try {
            guard = cache.get(m);
            if (guard == null) {
                String expr = ganno.value();
                if (expr.length() > 0) {
                    guard = PermissionService.parse(ganno.value());
                } else {
                    guard = (Guard)ganno.type().newInstance();
                }
                cache.put(m , guard);
            }
        } catch (Exception e) {
            throw WebException.wrap("Failed to check guard", e);
        }
        if (!guard.check(rs)) {
            throw new WebSecurityException("Access denied to method "
                    +ctx.getRequest().getHttpMethod()+" of resource "+ rs.getPath());
        }
    }

}
