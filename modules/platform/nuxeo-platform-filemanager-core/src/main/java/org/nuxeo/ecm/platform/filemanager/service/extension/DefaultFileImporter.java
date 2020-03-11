/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.filemanager.service.extension;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET;

import org.apache.commons.lang3.StringUtils;
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
            if (StringUtils.isNotBlank(defaultType)) {
                return defaultType;
            }
        }
        return TYPE_NAME;
    }

    protected static UITypesConfiguration getConfiguration(DocumentModel currentDoc) {
        LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);
        return localConfigurationService.getConfiguration(UITypesConfiguration.class, UI_TYPES_CONFIGURATION_FACET,
                currentDoc);
    }

}
