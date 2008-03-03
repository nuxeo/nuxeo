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
 * $Id$
 */

package org.nuxeo.platform.cache.web;

import java.io.Serializable;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;

/**
 * This interceptor is used to reactivate cache listeners.
 */
public class CacheNotifierInterceptor implements Serializable {

    private static final long serialVersionUID = -6776067889976399389L;

    private static final Log log = LogFactory.getLog(CacheNotifierInterceptor.class);

    @AroundInvoke
    public Object awakeCacheNotifier(InvocationContext invocation)
            throws Exception {

        final Context sessionContext = Contexts.getSessionContext();

        if (sessionContext != null) {
            final CacheUpdateNotifier cacheUpdateNotifier = (CacheUpdateNotifier) sessionContext
                    .get(CacheUpdateNotifier.SEAM_NAME_CACHE_NOTIFIER);

            if (cacheUpdateNotifier != null) {
                cacheUpdateNotifier.doNothing();
            } else {
                log.debug(CacheUpdateNotifier.SEAM_NAME_CACHE_NOTIFIER
                        + " not found on session context");
            }
        } else {
            log.debug("Couldn't get "
                    + CacheUpdateNotifier.SEAM_NAME_CACHE_NOTIFIER
                    + ", sessionContext does not exist");
        }

        log.debug("Before invocation...");
        return invocation.proceed();
    }

}
