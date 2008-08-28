/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.nuxeo.ecm.webengine.rest.servlet.jersey.patch;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.uri.ExtendedUriInfo;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.sun.jersey.spi.service.ComponentContext;
import com.sun.jersey.spi.service.ComponentProvider.Scope;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public final class HttpContextInjectableProvider implements
        InjectableProvider<Context, Type> {

    private static final class HttpContextInjectable implements Injectable<Object> {
        public Object getValue(HttpContext context) {
            return context;
        }
    }

    private static final class HttpContextRequestInjectable implements Injectable<Object> {
        public Object getValue(HttpContext context) {
            return context.getRequest();
        }
    }

    private static final class UriInfoInjectable implements Injectable<UriInfo> {
        public UriInfo getValue(HttpContext context) {
            return context.getUriInfo();
        }
    }

    protected final Map<Type, Injectable> injectables;

    public HttpContextInjectableProvider() {
        injectables = new HashMap<Type, Injectable>();

        HttpContextRequestInjectable re = new HttpContextRequestInjectable();
        injectables.put(HttpHeaders.class, re);
        injectables.put(Request.class, re);
        injectables.put(SecurityContext.class, re);

        injectables.put(HttpContext.class, new HttpContextInjectable());

        injectables.put(UriInfo.class, new UriInfoInjectable());
        injectables.put(ExtendedUriInfo.class, new UriInfoInjectable());
    }

    public Scope getScope() {
        return Scope.PerRequest;
    }

    public Injectable getInjectable(ComponentContext ic, Context a, Type c) {
        return injectables.get(c);
    }

    public void putInjectable(Class<?> klass, Injectable<?> injectable) {
        injectables.put(klass, injectable);
    }
}