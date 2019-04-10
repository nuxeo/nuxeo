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

package org.nuxeo.dam.importer.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dam.core.service.InheritedPropertiesDescriptor;
import org.nuxeo.dam.core.service.InheritedPropertiesService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.properties.MetadataFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to use the {@code MetadataFile} class with DAM.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class MetadataFileHelper {

    private MetadataFileHelper() {
        // helper class
    }

    private static final Log log = LogFactory.getLog(MetadataFileHelper.class);

    protected static InheritedPropertiesService inheritedPropertiesService;

    protected static InheritedPropertiesService getInheritedPropertiesService() {
        if (inheritedPropertiesService == null) {
            try {
                inheritedPropertiesService = Framework.getService(InheritedPropertiesService.class);
            } catch (Exception e) {
                log.error("Unable to retrieve InheritedPropertiesService", e);
            }
        }
        return inheritedPropertiesService;
    }

    /**
     * Returns a new {@code MetadataFile} object created from the given {@code
     * doc} and using the properties registered through the {@code
     * InheritedPropertiesService}.
     */
    public static MetadataFile createFrom(DocumentModel doc)
            throws ClientException {
        InheritedPropertiesService service = getInheritedPropertiesService();
        Map<String, InheritedPropertiesDescriptor> inheritedPropertiesDescriptors = service.getInheritedPropertiesDescriptors();

        List<String> schemas = new ArrayList<String>();
        List<String> properties = new ArrayList<String>();
        for (InheritedPropertiesDescriptor descriptor : inheritedPropertiesDescriptors.values()) {
            if (descriptor.allProperties()) {
                schemas.add(descriptor.getSchema());
            } else {
                properties.addAll(descriptor.getProperties());
            }
        }
        return MetadataFile.createFromSchemasAndProperties(doc, schemas,
                properties);
    }

}
