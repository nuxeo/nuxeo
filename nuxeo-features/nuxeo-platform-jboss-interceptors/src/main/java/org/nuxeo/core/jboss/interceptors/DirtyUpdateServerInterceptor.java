/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.core.jboss.interceptors;

import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.nuxeo.common.DirtyUpdateInvokeBridge;

import static org.nuxeo.core.jboss.interceptors.DirtyUpdateConstants.ATTRIBUTE;
import static org.nuxeo.core.jboss.interceptors.DirtyUpdateConstants.TAG;

public class DirtyUpdateServerInterceptor implements Interceptor {

    public String getName() {
        return getClass().getSimpleName();
    }

    public Object invoke(Invocation invocation) throws Throwable {
        Object tag = invocation.getMetaData(TAG, ATTRIBUTE);
        if (tag == null) { // protect from local invoke
            return invocation.invokeNext();
        }
        try {
            DirtyUpdateInvokeBridge.putTagInThreadContext(tag);
            return invocation.invokeNext();
        } finally {
            DirtyUpdateInvokeBridge.clearThreadContext();
        }
    }
}
