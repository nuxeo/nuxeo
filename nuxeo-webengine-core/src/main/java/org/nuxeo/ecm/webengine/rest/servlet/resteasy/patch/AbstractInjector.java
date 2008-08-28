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

package org.nuxeo.ecm.webengine.rest.servlet.resteasy.patch;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.resteasy.CookieParamInjector;
import org.resteasy.HeaderParamInjector;
import org.resteasy.MatrixParamInjector;
import org.resteasy.MessageBodyParameterInjector;
import org.resteasy.PathParamIndex;
import org.resteasy.PathParamInjector;
import org.resteasy.QueryParamInjector;
import org.resteasy.ValueInjector;
import org.resteasy.spi.ResteasyProviderFactory;
import org.resteasy.util.FindAnnotation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AbstractInjector {

    public ValueInjector getParameterExtractor(PathParamIndex index,
            Class<?> type, Type genericType, Annotation[] annotations,
            AccessibleObject target, ResteasyProviderFactory providerFactory) {

        DefaultValue defaultValue = FindAnnotation.findAnnotation(annotations,
                DefaultValue.class);
        boolean encode = FindAnnotation.findAnnotation(annotations,
                Encoded.class) != null
                || target.isAnnotationPresent(Encoded.class)
                || type.isAnnotationPresent(Encoded.class);
        String defaultVal = null;
        if (defaultValue != null)
            defaultVal = defaultValue.value();

        QueryParam query;
        HeaderParam header;
        MatrixParam matrix;
        PathParam uriParam;
        CookieParam cookie;

        if ((query = FindAnnotation.findAnnotation(annotations,
                QueryParam.class)) != null) {
            return new QueryParamInjector(type, genericType, target,
                    query.value(), defaultVal, encode, query.encode());
        } else if ((header = FindAnnotation.findAnnotation(annotations,
                HeaderParam.class)) != null) {
            return new HeaderParamInjector(type, genericType, target,
                    header.value(), defaultVal);
        } else if ((cookie = FindAnnotation.findAnnotation(annotations,
                CookieParam.class)) != null) {
            return new CookieParamInjector(type, genericType, target,
                    cookie.value(), defaultVal);
        } else if ((uriParam = FindAnnotation.findAnnotation(annotations,
                PathParam.class)) != null) {
            return new PathParamInjector(index, type, genericType, target,
                    uriParam.value(), defaultVal, encode);
        } else if ((matrix = FindAnnotation.findAnnotation(annotations,
                MatrixParam.class)) != null) {
            return new MatrixParamInjector(type, genericType, target,
                    matrix.value(), defaultVal);
        } else if (FindAnnotation.findAnnotation(annotations, Context.class) != null) {
            return new ContextParameterInjector(type, providerFactory);
        } else {
            return new MessageBodyParameterInjector(type, genericType,
                    annotations, providerFactory);
        }
    }

}
