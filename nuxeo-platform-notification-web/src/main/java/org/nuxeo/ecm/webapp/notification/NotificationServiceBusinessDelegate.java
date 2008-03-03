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

package org.nuxeo.ecm.webapp.notification;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import javax.annotation.security.PermitAll;
import javax.interceptor.Interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.webapp.shield.ErrorHandlingInterceptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Business delegate that puts a reference to NotificationService on the seam context.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Name("notificationManager")
@Scope(SESSION)
@Interceptors(ErrorHandlingInterceptor.class)
public class NotificationServiceBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -6259519917542409836L;

    private static final Log log = LogFactory.getLog(NotificationServiceBusinessDelegate.class);

    // caching NotificationServiceRemote
    private NotificationManager notificationManager;

    //@Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link NotificationManager} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     *
     * @return
     * @throws ClientException
     */
    @Unwrap
    public NotificationManager getNotificationManager() throws ClientException {
        if (null == notificationManager) {
            try {
                notificationManager = Framework.getService(NotificationManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to NotificationService. "
                        + e.getMessage();
                //log.error(errMsg, e);
                throw new ClientException(errMsg, e);
            }

            if (null == notificationManager) {
                throw new ClientException("NotificationService not bound");
            }
        }
        return notificationManager;
    }

    @Destroy
    @PermitAll
    public void destroy() throws ClientException {
        if (null != notificationManager) {
            notificationManager = null;
        }
        log.info("Destroyed the seam component...");
    }

}
