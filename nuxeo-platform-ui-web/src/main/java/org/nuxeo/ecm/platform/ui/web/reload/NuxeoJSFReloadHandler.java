/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.reload;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * Handler for hot reload features.
 * <p>
 * Hot reload features should be enabled only if the application is in a
 * "debug" mode, as some reloading can hurt a server in production (when
 * altering document types declared on the platform, resetting caches, etc...)
 * <p>
 * Note that some hot reload features cannot be handled without additional
 * debug configurations to be available (like the seam debug jar for instance).
 * <p>
 * This reload handler is supposed to handle at least (or most of) features
 * used in Studio.
 *
 * @since 5.6
 */
public class NuxeoJSFReloadHandler implements EventListener {

    private static final Log log = LogFactory.getLog(NuxeoJSFReloadHandler.class);

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return true;
    }

    @Override
    public void handleEvent(Event event) {
        if (!Framework.isDevModeSet()) {
            log.info("Do not flush the JSF application: debug mode is not set");
            return;
        }
        String id = event.getId();
        if (ReloadEventNames.FLUSH_EVENT_ID.equals(id)) {
            // force i18n messages reload at the bundle level
            ResourceBundle.clearCache(Thread.currentThread().getContextClassLoader());
            // force reload of document default views.
            DocumentModelFunctions.resetDefaultViewCache();
        }
    }

}
