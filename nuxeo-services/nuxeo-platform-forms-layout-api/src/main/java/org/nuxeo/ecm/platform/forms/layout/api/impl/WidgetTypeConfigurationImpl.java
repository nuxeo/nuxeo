/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;

public class WidgetTypeConfigurationImpl implements WidgetTypeConfiguration {

    private static final long serialVersionUID = 1L;

    String title;

    String description;

    boolean list = false;

    boolean complex = false;

    List<String> supportedFieldTypes;

    List<String> defaultFieldTypes;

    List<String> categories;

    Map<String, List<LayoutDefinition>> propertyLayouts;

    public WidgetTypeConfigurationImpl(String title, String description,
            boolean list, boolean complex, List<String> supportedFieldTypes,
            List<String> defaultFieldTypes, List<String> categories,
            Map<String, List<LayoutDefinition>> propertyLayouts) {
        super();
        this.title = title;
        this.description = description;
        this.list = list;
        this.complex = complex;
        this.supportedFieldTypes = supportedFieldTypes;
        this.defaultFieldTypes = defaultFieldTypes;
        this.categories = categories;
        this.propertyLayouts = propertyLayouts;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isList() {
        return list;
    }

    public boolean isComplex() {
        return complex;
    }

    public List<String> getSupportedFieldTypes() {
        return supportedFieldTypes;
    }

    public List<String> getDefaultFieldTypes() {
        return defaultFieldTypes;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        return propertyLayouts;
    }

    public List<LayoutDefinition> getPropertyLayouts(String mode,
            String additionalMode) {
        if (propertyLayouts != null) {
            List<LayoutDefinition> res = new ArrayList<LayoutDefinition>();
            if (additionalMode != null) {
                List<LayoutDefinition> defaultLayouts = propertyLayouts.get(additionalMode);
                if (defaultLayouts != null) {
                    res.addAll(defaultLayouts);
                }
            }
            List<LayoutDefinition> modeLayouts = propertyLayouts.get(mode);
            if (modeLayouts != null) {
                res.addAll(modeLayouts);
            }
            return res;
        }
        return null;
    }

}
