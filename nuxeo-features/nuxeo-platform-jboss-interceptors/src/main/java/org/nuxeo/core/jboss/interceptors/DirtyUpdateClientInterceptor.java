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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.aop.advice.Interceptor;
import org.jboss.aop.joinpoint.Invocation;
import org.jboss.ejb3.stateful.StatefulRemoteInvocation;
import org.nuxeo.common.DirtyUpdateInvokeBridge;

import static org.nuxeo.core.jboss.interceptors.DirtyUpdateConstants.ATTRIBUTE;
import static org.nuxeo.core.jboss.interceptors.DirtyUpdateConstants.TAG;

public class DirtyUpdateClientInterceptor implements Interceptor, Serializable {

    public static final Log log = LogFactory.getLog(DirtyUpdateClientInterceptor.class);

    private static final long serialVersionUID = 1L;

    public String getName() {
        return getClass().getSimpleName();
    }

    protected void addDirtyTag(StatefulRemoteInvocation invocation) {
        Object tag = DirtyUpdateInvokeBridge.getThreadContext().tag;
        invocation.getMetaData().addMetaData(TAG, ATTRIBUTE, tag);
    }

    public Object invoke(Invocation invocation) throws Throwable {
        addDirtyTag((StatefulRemoteInvocation) invocation);
        return invocation.invokeNext();
    }
}
