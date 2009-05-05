/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.picture.api.adapters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.picture.config.PictureConfigurationService;
import org.nuxeo.runtime.api.Framework;


public class PictureResourceAdapterFactory implements DocumentAdapterFactory {

    private static PictureConfigurationService configService;

    private static final Log log = LogFactory.getLog(PictureResourceAdapterFactory.class);

    private static PictureConfigurationService getConfigService() {
        if (configService == null) {
            configService = (PictureConfigurationService) Framework.getRuntime().getComponent(
                    PictureConfigurationService.NAME);
        }
        return configService;
    }

    public Object getAdapter(DocumentModel doc, Class cls) {

        PictureResourceAdapter adapter = null;

        // first try to get Type Adapter
        try {
            adapter = getConfigService().getAdapterForType(doc.getType());
        } catch (InstantiationException e) {
            log.error("Error while getting PICTURE adapter for type "
                    + doc.getType() + ':' + e.getMessage());
        } catch (IllegalAccessException e) {
            log.error("Error while getting PICTURE adapter for type "
                    + doc.getType() + ':' + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Error while getting PICTUREAdapter Configuration Service" + ':' + e.getMessage());
        }

        if (adapter == null) {
            adapter = new DefaultPictureAdapter();
        }

        adapter.setDocumentModel(doc);
        return adapter;
    }

}
