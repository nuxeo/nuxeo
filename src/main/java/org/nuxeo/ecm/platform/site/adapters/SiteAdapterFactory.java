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

package org.nuxeo.ecm.platform.site.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.site.api.SiteAdaptersManager;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory for Site resources adapters.
 * <p>
 * Uses an extension point to find the adapter implementation according to
 * document type.
 *
 * @author tiry
 */
public class SiteAdapterFactory implements DocumentAdapterFactory {

    private static SiteAdaptersManager siteAdapterService;

    private static final Log log = LogFactory.getLog(SiteAdapterFactory.class);

    private static SiteAdaptersManager getSiteAdapterService() {
        if (siteAdapterService == null) {
            siteAdapterService = Framework.getLocalService(SiteAdaptersManager.class);
        }
        return siteAdapterService;
    }

    public Object getAdapter(DocumentModel doc, Class cls) {

        SiteAwareObject adapter = null;

        // first try to get Type Adapter
        try {
            adapter = getSiteAdapterService().getSiteAdapterForType(doc);
        } catch (Exception e) {
            log.error("Error while getting Site adapter for type "
                    + doc.getType() + ':' + e.getMessage());
        }

        if (adapter == null) {
            // use default adapters
            if (doc.isFolder()) {
                adapter = new FolderishSiteObjectHandler(doc);
            } else {
                adapter = new DefaultSiteObjectHandler(doc);
            }
        }
        return adapter;
    }

}
