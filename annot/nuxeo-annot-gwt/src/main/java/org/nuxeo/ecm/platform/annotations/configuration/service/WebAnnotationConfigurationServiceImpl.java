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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.annotations.gwt.server.configuration.UserInfoMapper;
import org.nuxeo.ecm.platform.annotations.gwt.server.configuration.WebPermission;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 *
 */
public class WebAnnotationConfigurationServiceImpl extends DefaultComponent
        implements WebAnnotationConfigurationService {

    private static final Log log = LogFactory.getLog(WebAnnotationConfigurationServiceImpl.class);

    private static final String ANNOTATION_TYPES_EXTENSION_POINT = "types";

    private static final String USER_INFO_EXTENSION_POINT = "userInfo";

    private static final String WEB_PERMISSION_EXTENSION_POINT = "webPermission";

    private static final String FILTERS_EXTENSION_POINT = "filters";

    private static final String DISPLAYED_FIELDS_EXTENSION_POINT = "displayedFields";

    private Map<String, WebAnnotationDefinitionDescriptor> annotationDefinitionsDescriptors;

    private UserInfoMapper userInfoMapper;

    private WebPermission webPermission;

    private Map<String, FilterDescriptor> filterDescriptors;

    private Set<String> displayedFields;

    private Map<String, String> fieldLabels;

    @Override
    public void activate(ComponentContext context) throws Exception {
        annotationDefinitionsDescriptors = new HashMap<String, WebAnnotationDefinitionDescriptor>();
        filterDescriptors = new HashMap<String, FilterDescriptor>();
        displayedFields = new HashSet<String>();
        fieldLabels = new HashMap<String, String>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        annotationDefinitionsDescriptors = null;
        userInfoMapper = null;
        filterDescriptors = null;
        displayedFields = null;
        fieldLabels = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ANNOTATION_TYPES_EXTENSION_POINT.equals(extensionPoint)) {
            WebAnnotationDefinitionDescriptor descriptor = (WebAnnotationDefinitionDescriptor) contribution;
            if (annotationDefinitionsDescriptors.put(descriptor.getName(),
                    descriptor) != null) {
                log.info("Already registered annotation type: "
                        + descriptor.getName() + ", storing the new one.");
            }
        } else if (USER_INFO_EXTENSION_POINT.equals(extensionPoint)) {
            UserInfoMapperDescriptor descriptor = (UserInfoMapperDescriptor) contribution;
            userInfoMapper = descriptor.getKlass().newInstance();
        } else if (WEB_PERMISSION_EXTENSION_POINT.equals(extensionPoint)) {
            WebPermissionDescriptor descriptor = (WebPermissionDescriptor) contribution;
            webPermission = descriptor.getKlass().newInstance();
        } else if (FILTERS_EXTENSION_POINT.equals(extensionPoint)) {
            FilterDescriptor descriptor = (FilterDescriptor) contribution;
            if (filterDescriptors.put(descriptor.getName(), descriptor) != null) {
                log.info("Already registered annotation filter: "
                        + descriptor.getName() + ", storing the new one.");
            }
        } else if (DISPLAYED_FIELDS_EXTENSION_POINT.equals(extensionPoint)) {
            DisplayedFieldsDescriptor descriptor = (DisplayedFieldsDescriptor) contribution;
            String fieldName = descriptor.getName();
            if (descriptor.isDisplayed()) {
                displayedFields.add(fieldName);
            } else {
                displayedFields.remove(fieldName);
            }

            String fieldLabel = descriptor.getLabel();
            if (fieldLabel != null) {
                fieldLabels.put(fieldName, fieldLabel);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ANNOTATION_TYPES_EXTENSION_POINT.equals(extensionPoint)) {
            WebAnnotationDefinitionDescriptor descriptor = (WebAnnotationDefinitionDescriptor) contribution;
            annotationDefinitionsDescriptors.remove(descriptor.getName());
        } else if (FILTERS_EXTENSION_POINT.equals(extensionPoint)) {
            FilterDescriptor descriptor = (FilterDescriptor) contribution;
            filterDescriptors.remove(descriptor.getName());
        } else if (DISPLAYED_FIELDS_EXTENSION_POINT.equals(extensionPoint)) {
            DisplayedFieldsDescriptor descriptor = (DisplayedFieldsDescriptor) contribution;
            String fieldName = descriptor.getName();
            displayedFields.remove(fieldName);
            fieldLabels.remove(fieldName);
        }
    }

    public List<WebAnnotationDefinitionDescriptor> getAllWebAnnotationDefinitions() {
        return new ArrayList<WebAnnotationDefinitionDescriptor>(
                annotationDefinitionsDescriptors.values());
    }

    public List<WebAnnotationDefinitionDescriptor> getEnabledWebAnnotationDefinitions() {
        List<WebAnnotationDefinitionDescriptor> definitions = new ArrayList<WebAnnotationDefinitionDescriptor>();
        for (WebAnnotationDefinitionDescriptor def : annotationDefinitionsDescriptors.values()) {
            if (def.isEnabled()) {
                definitions.add(def);
            }
        }
        return definitions;
    }

    public UserInfoMapper getUserInfoMapper() {
        return userInfoMapper;
    }

    public WebPermission getWebPermission() {
        return webPermission;
    }

    public Map<String, FilterDescriptor> getFilterDefinitions() {
        return filterDescriptors;
    }

    public Set<String> getDisplayedFields() {
        return displayedFields;
    }

    public Map<String, String> getFieldLabels() {
        return fieldLabels;
    }

}
