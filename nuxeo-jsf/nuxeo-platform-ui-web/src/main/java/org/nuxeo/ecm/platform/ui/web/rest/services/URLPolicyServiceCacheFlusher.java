/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.rest.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Event listener that flushes the {@link URLPolicyService} cache.
 *
 * @since 5.5
 */
public class URLPolicyServiceCacheFlusher implements EventListener {

    private static final Log log = LogFactory.getLog(URLPolicyServiceCacheFlusher.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (!Framework.isDevModeSet()) {
            log.info("Do not flush the URL policy service: dev mode is not set");
            return;
        }
        if (!ReloadEventNames.FLUSH_EVENT_ID.equals(event.getId())) {
            return;
        }
        try {
            URLPolicyService service = Framework.getService(URLPolicyService.class);
            service.flushCache();
        } catch (Exception e) {
            log.error("Error while flushing the URLPolicyService cache", e);
        }
    }

}
