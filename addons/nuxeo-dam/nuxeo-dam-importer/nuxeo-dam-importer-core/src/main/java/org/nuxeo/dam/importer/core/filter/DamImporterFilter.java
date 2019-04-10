/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.dam.importer.core.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.platform.importer.filter.ImporterFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Disable not needed DAM InitPropertiesListener as the Importer will take care
 * of the metadata to set on the assets.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamImporterFilter implements ImporterFilter {

    private static final Log log = LogFactory.getLog(DamImporterFilter.class);

    public static final String DAM_INIT_PROPERTIES_LISTENER = "damInitPropertiesListener";

    protected EventServiceAdmin eventAdmin;

    public void handleBeforeImport() {
        eventAdmin = Framework.getLocalService(EventServiceAdmin.class);

        if (eventAdmin != null) {
            eventAdmin.setListenerEnabledFlag(DAM_INIT_PROPERTIES_LISTENER,
                    false);
        } else {
            log.warn("EventServiceAdmin service was not found ... Possible that the import process will not proceed ok");
        }
    }

    public void handleAfterImport(Exception e) {
        eventAdmin = Framework.getLocalService(EventServiceAdmin.class);

        if (eventAdmin != null) {
            eventAdmin.setListenerEnabledFlag(DAM_INIT_PROPERTIES_LISTENER,
                    true);
        } else {
            log.warn("EventServiceAdmin service was not found ... Possible that the import process will not proceed ok");
        }
    }

}
