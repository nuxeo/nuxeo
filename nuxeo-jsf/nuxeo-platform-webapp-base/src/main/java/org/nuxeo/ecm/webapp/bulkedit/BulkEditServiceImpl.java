/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.webapp.bulkedit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@link BulkEditService}.
 *
 * @since 5.7.3
 */
public class BulkEditServiceImpl extends DefaultComponent implements BulkEditService {

    public static final String VERSIONING_EP = "versioning";

    public static final VersioningOption DEFAULT_VERSIONING_OPTION = VersioningOption.MINOR;

    private static final Log log = LogFactory.getLog(BulkEditServiceImpl.class);

    protected VersioningOption defaultVersioningOption = DEFAULT_VERSIONING_OPTION;

    @Override
    public void updateDocuments(CoreSession session, DocumentModel sourceDoc, List<DocumentModel> targetDocs)
            {
        List<String> propertiesToCopy = getPropertiesToCopy(sourceDoc);
        if (propertiesToCopy.isEmpty()) {
            return;
        }

        for (DocumentModel targetDoc : targetDocs) {

            for (String propertyToCopy : propertiesToCopy) {
                try {
                    targetDoc.setPropertyValue(propertyToCopy, sourceDoc.getPropertyValue(propertyToCopy));
                } catch (PropertyNotFoundException e) {
                    String message = "%s property does not exist on %s";
                    log.warn(String.format(message, propertyToCopy, targetDoc));
                }
            }
            targetDoc.putContextData(VersioningService.VERSIONING_OPTION, defaultVersioningOption);
            session.saveDocument(targetDoc);
        }
    }

    /**
     * Extracts the properties to be copied from {@code sourceDoc}. The properties are stored in the ContextData of
     * {@code sourceDoc}: the key is the xpath property, the value is {@code true} if the property has to be copied,
     * {@code false otherwise}.
     */
    protected List<String> getPropertiesToCopy(DocumentModel sourceDoc) {
        List<String> propertiesToCopy = new ArrayList<>();
        for (Map.Entry<String, Serializable> entry : sourceDoc.getContextData().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(BULK_EDIT_PREFIX)) {
                String[] properties = key.replace(BULK_EDIT_PREFIX, "").split(" ");
                Serializable value = entry.getValue();
                if (value instanceof Boolean && (Boolean) value) {
                    for (String property : properties) {
                        if (!property.startsWith(CONTEXT_DATA)) {
                            propertiesToCopy.add(property);
                        }
                    }
                }
            }
        }
        return propertiesToCopy;
    }

    /**
     * @deprecated since 7.3. The option is passed to the CoreSession#saveDocument method.
     */
    @Deprecated
    protected void checkIn(DocumentModel doc) {
        if (defaultVersioningOption != null && defaultVersioningOption != VersioningOption.NONE) {
            if (doc.isCheckedOut()) {
                doc.checkIn(defaultVersioningOption, null);
            }
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (VERSIONING_EP.equals(extensionPoint)) {
            VersioningDescriptor desc = (VersioningDescriptor) contribution;
            String defaultVer = desc.getDefaultVersioningOption();
            if (!StringUtils.isBlank(defaultVer)) {
                try {
                    defaultVersioningOption = VersioningOption.valueOf(defaultVer.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    log.warn(String.format("Illegal versioning option: %s, using %s instead", defaultVer,
                            DEFAULT_VERSIONING_OPTION));
                    defaultVersioningOption = DEFAULT_VERSIONING_OPTION;
                }
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (VERSIONING_EP.equals(extensionPoint)) {
            defaultVersioningOption = DEFAULT_VERSIONING_OPTION;
        }
    }

}
