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

package org.nuxeo.ecm.platform.site.adapters;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.api.SiteAdaptersManager;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class SiteAdaptersManagerService extends DefaultComponent implements
        SiteAdaptersManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.site.adapters.SiteAdaptersManagerService");

    private static final Log log = LogFactory.getLog(SiteAdaptersManagerService.class);

    public static final String SITE_ADAPTER_EP = "siteAdapter";

    private static Map<String, SiteAdapterDescriptor> siteAdapterDescriptors = new HashMap<String, SiteAdapterDescriptor>();

    // Registry and EP management

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (SITE_ADAPTER_EP.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                SiteAdapterDescriptor descriptor = (SiteAdapterDescriptor) contrib;
                try {
                    registerSiteAdapter(descriptor);
                } catch (Exception e) {
                    log.error(
                            "Error during site type adapter registration for type "
                                    + descriptor.getDocType(), e);
                }
            }

        }
    }

    protected void registerSiteAdapter(SiteAdapterDescriptor descriptor) {
        String docType = descriptor.getDocType();
        if (docType == null) {
            log.error("Unable to register a siteAdapter against a null docType");
            return;
        }
        siteAdapterDescriptors.put(docType, descriptor);
    }

    // Service interface

    public SiteAwareObject getSiteAdapterForType(DocumentModel doc)
            throws Exception {
        String docType = doc.getType();

        SiteAdapterDescriptor desc = siteAdapterDescriptors.get(docType);

        if (desc == null)
            return null;

        SiteAwareObject adapter = desc.getNewInstance();

        if (adapter != null)
            adapter.setSourceDocument(doc);

        return adapter;
    }

}
