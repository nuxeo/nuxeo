/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DEFAULT_TYPE;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DENIED_TYPES_PROPERTY;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DENY_ALL_TYPES_PROPERTY;
import static org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants.UI_TYPES_DEFAULT_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.localconfiguration.AbstractLocalConfiguration;
import org.nuxeo.ecm.platform.types.SubType;

/**
 * Default implementation of {@code UITypesConfiguration}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class UITypesConfigurationAdapter extends AbstractLocalConfiguration<UITypesConfiguration>
        implements UITypesConfiguration {

    private static final Log log = LogFactory.getLog(UITypesConfigurationAdapter.class);

    protected DocumentRef documentRef;

    protected List<String> allowedTypes;

    protected List<String> deniedTypes;

    protected boolean denyAllTypes;

    protected boolean canMerge = true;

    protected String defaultType;

    public UITypesConfigurationAdapter(DocumentModel doc) {
        documentRef = doc.getRef();
        allowedTypes = getTypesList(doc, UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY);
        deniedTypes = getTypesList(doc, UI_TYPES_CONFIGURATION_DENIED_TYPES_PROPERTY);
        defaultType = getDefaultType(doc);

        denyAllTypes = getDenyAllTypesProperty(doc);
        if (denyAllTypes) {
            canMerge = false;
        }
    }

    protected List<String> getTypesList(DocumentModel doc, String property) {
        String[] types;
        try {
            types = (String[]) doc.getPropertyValue(property);
        } catch (PropertyException e) {
            return Collections.emptyList();
        }
        if (types == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(types));
    }

    protected boolean getDenyAllTypesProperty(DocumentModel doc) {
        try {
            Boolean value = (Boolean) doc.getPropertyValue(UI_TYPES_CONFIGURATION_DENY_ALL_TYPES_PROPERTY);
            return Boolean.TRUE.equals(value);
        } catch (PropertyException e) {
            return false;
        }
    }

    protected String getDefaultType(DocumentModel doc) {
        String value = UI_TYPES_DEFAULT_TYPE;
        try {
            value = (String) doc.getPropertyValue(UI_TYPES_CONFIGURATION_DEFAULT_TYPE);
        } catch (PropertyException e) {
            log.debug("can't get default type for:" + doc.getPathAsString(), e);
        }
        return value;
    }

    @Override
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    @Override
    public List<String> getDeniedTypes() {
        return deniedTypes;
    }

    @Override
    public boolean denyAllTypes() {
        return denyAllTypes;
    }

    @Override
    public DocumentRef getDocumentRef() {
        return documentRef;
    }

    @Override
    public boolean canMerge() {
        return canMerge;
    }

    @Override
    public UITypesConfiguration merge(UITypesConfiguration other) {
        if (other == null) {
            return this;
        }

        // set the documentRef to the other UITypesConfiguration to continue
        // merging, if needed
        documentRef = other.getDocumentRef();

        if (allowedTypes.isEmpty()) {
            allowedTypes = Collections.unmodifiableList(new ArrayList<>(other.getAllowedTypes()));
        }

        List<String> deniedTypes = new ArrayList<>(this.deniedTypes);
        deniedTypes.addAll(other.getDeniedTypes());
        this.deniedTypes = Collections.unmodifiableList(deniedTypes);

        denyAllTypes = other.denyAllTypes();
        if (denyAllTypes) {
            canMerge = false;
        }

        return this;
    }

    @Override
    public Map<String, SubType> filterSubTypes(Map<String, SubType> allowedSubTypes) {
        if (denyAllTypes()) {
            return Collections.emptyMap();
        }

        List<String> allowedTypes = getAllowedTypes();
        List<String> deniedTypes = getDeniedTypes();
        if (allowedTypes.isEmpty() && deniedTypes.isEmpty()) {
            return allowedSubTypes;
        }

        Map<String, SubType> filteredAllowedSubTypes = new HashMap<>(allowedSubTypes);
        filteredAllowedSubTypes.keySet().removeIf(subTypeName -> deniedTypes.contains(subTypeName)
                || !allowedTypes.isEmpty() && !allowedTypes.contains(subTypeName));
        return filteredAllowedSubTypes;
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfiguration# getDefaultType()
     */
    @Override
    public String getDefaultType() {
        return defaultType;
    }

}
