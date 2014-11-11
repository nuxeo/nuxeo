/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.ejb;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A statistical interceptor to measure the backend utilization
 * when cache is involved aside from the case when there is no cache.
 * <p>
 * This should give a stat with the counts for each method annotated
 * with this interceptor.
 *
 * @author DM
 */
public class DocumentManagerCacheStatInterceptor {

    private static final Log log = LogFactory.getLog(DocumentManagerCacheStatInterceptor.class);

    /**
     * Counts the calls to each methods.
     */
    private static final Map<String, Integer> methodHits = new HashMap<String, Integer>();

    @AroundInvoke
    public Object countCall(InvocationContext invocationContext)
            throws Exception {

        final Method method = invocationContext.getMethod();

        // final String methodName = method.getName();

        final String methodSignature = method.toString();

        Integer count = methodHits.get(methodSignature);
        if (count == null) {
            count = 1;
        } else {
            count++;
        }
        methodHits.put(methodSignature, count);

        log.debug("Calls # " + count + " for method '" + methodSignature);

        return invocationContext.proceed();
    }
}
