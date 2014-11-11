/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration;
import org.nuxeo.runtime.api.Framework;

/**
 * Default file importer, creating a regular file.
 */
public class DefaultFileImporter extends AbstractFileImporter {

    public static final String TYPE_NAME = "File";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DefaultFileImporter.class);

    @Override
    public boolean isOverwriteByTitle() {
        return false; // by filename
    }

    @Override
    public String getDocType(DocumentModel container) {
        String type = super.getDocType(container);
        if (type == null) {
            type = getTypeName(container);
        }
        return type;
    }

    public static String getTypeName(DocumentModel currentDoc) {
        UITypesConfiguration configuration = getConfiguration(currentDoc);
        if (configuration != null) {
            String defaultType = configuration.getDefaultType();
            if (defaultType != null) {
                return defaultType;
            }
        }
        return TYPE_NAME;
    }

    protected static UITypesConfiguration getConfiguration(
            DocumentModel currentDoc) {
        UITypesConfiguration configuration = null;
        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
            configuration = localConfigurationService.getConfiguration(
                    UITypesConfiguration.class, UI_TYPES_CONFIGURATION_FACET,
                    currentDoc);
        } catch (Exception e) {
            log.error(e, e);
        }
        return configuration;
    }

}
