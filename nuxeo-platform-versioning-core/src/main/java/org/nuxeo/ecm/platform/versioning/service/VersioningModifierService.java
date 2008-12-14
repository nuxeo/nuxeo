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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.versioning.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * TODO: DOCUMENT ME.
 *
 * @author <a href="mailto:bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public class VersioningModifierService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.versioning.service.VersioningModifierService");

    private static final Log log = LogFactory.getLog(VersioningModifierService.class);

    private Map<String, List<VersioningModifierPropertyDescriptor>> modifications;

    @Override
    public void activate(ComponentContext context) {
        log.info("Activate versioningModifer extension");
        modifications = new HashMap<String, List<VersioningModifierPropertyDescriptor>>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        modifications = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("versioningModifier")) {
            log.info("Registering versioningModifer contribution");
            try {
                VersioningModifierDescriptor versionModifierDesc = (VersioningModifierDescriptor) contribution;
                List<VersioningModifierPropertyDescriptor> properties = registerProperties(versionModifierDesc);
                modifications.put(versionModifierDesc.getDocumentType(),
                        properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("versioningModifier")) {
            log.info("Unregistering versioningModifer contribution");
            try {
                VersioningModifierDescriptor versionModifierDesc = (VersioningModifierDescriptor) contribution;
                modifications.remove(versionModifierDesc.getDocumentType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<VersioningModifierPropertyDescriptor> registerProperties(
            VersioningModifierDescriptor versionModiferDesc) {
        List<VersioningModifierPropertyDescriptor> properties = new ArrayList<VersioningModifierPropertyDescriptor>();
        for (VersioningModifierPropertyDescriptor property : versionModiferDesc.getProperties()) {
            properties.add(property);
        }
        return properties;
    }

    public void doModifications(DocumentModel document) {
        if (document != null && modifications != null
                && !modifications.isEmpty()) {
            List<VersioningModifierPropertyDescriptor> properties = modifications.get(document.getType());

            if (properties == null || properties.isEmpty()) {
                log.debug("No properties modifications for document type "
                        + document.getType());
            } else {
                for (VersioningModifierPropertyDescriptor property : properties) {
                    performModifications(document, property.getSchema(),
                            property.getFieldname(), property.getAction());
                }
            }
        }
    }

    private static void performModifications(DocumentModel document,
            String schema, String fieldname, String action) {
        log.debug("performing  " + action + " on " + document.getId());

        // Custom action should be defined here
        if ("reset".equals(action)) {
            performReset(document, schema, fieldname);
        }
    }

    private static void performReset(DocumentModel document, String schema,
            String fieldname) {
        if (document != null && !"".equals(schema) && !"".equals(fieldname)) {
            try {
                document.setProperty(schema, fieldname, null);
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            log.debug(schema + ':' + fieldname + " resetted");
        }
    }

}
