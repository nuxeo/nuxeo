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

package org.nuxeo.platform.login.jboss;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.plugins.JaasSecurityManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Handle JAAS principal cache flushing on principal data edition.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public class JaasCacheFlusher implements EventListener {

    private static final Log log = LogFactory.getLog(JaasCacheFlusher.class);

    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    public void handleEvent(Event event) {
        if ("user_changed".equals(event.getId())
                || "group_changed".equals(event.getId())) {
            try {
                flushCache();
            } catch (Exception e) {
                log.error("error trying to flush JaasSecurityManager for "
                        + event.getData(), e);
            }
        }
    }

    public static void flushCache() throws Exception {
        JaasSecurityManager mgr = (JaasSecurityManager) new InitialContext().lookup("java:/jaas/nuxeo-ecm");
        mgr.flushCache();
    }

}
