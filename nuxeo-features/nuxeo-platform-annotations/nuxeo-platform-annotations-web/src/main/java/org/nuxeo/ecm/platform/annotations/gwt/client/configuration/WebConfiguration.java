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

package org.nuxeo.ecm.platform.annotations.gwt.client.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.filter.AnnotationDefinitionFilter;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class WebConfiguration implements IsSerializable {

    public static final WebConfiguration DEFAULT_WEB_CONFIGURATION;

    static {
        DEFAULT_WEB_CONFIGURATION = new WebConfiguration();
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Example", "Example",
                "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Comment", "Comment",
                "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#SeeAlso", "SeeAlso",
                "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Question",
                "Question", "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Explanation",
                "Explanation", "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Change", "Change",
                "icons/annotation-icon-Comment.png", "local"));
        DEFAULT_WEB_CONFIGURATION.addAnnotationDefinition(new AnnotationDefinition(
                "http://www.w3.org/2000/10/annotationType#Advice", "Advice",
                "icons/annotation-icon-Comment.png", "local"));
    }

    private Map<String, AnnotationDefinition> annotationDefinitions = new HashMap<String, AnnotationDefinition>();

    private Map<String, String> userInfo = new HashMap<String, String>();

    private List<AnnotationFilter> filters = new ArrayList<AnnotationFilter>();

    private Set<String> displayedFields = new HashSet<String>();

    private Map<String, String> fieldLabels;

    private boolean canAnnotate = true;

    public void addAnnotationDefinition(
            AnnotationDefinition annotationDefinition) {
        annotationDefinitions.put(annotationDefinition.getName(),
                annotationDefinition);
    }

    public void removeAnnotationDefinition(
            AnnotationDefinition annotationDefinition) {
        annotationDefinitions.remove(annotationDefinition);
    }

    public List<AnnotationDefinition> getAnnotationDefinitions() {
        List<AnnotationDefinition> list = new ArrayList<AnnotationDefinition>(
                annotationDefinitions.values());
        return Collections.unmodifiableList(list);
    }

    public List<AnnotationDefinition> getAnnotationDefinitions(
            AnnotationDefinitionFilter filter) {
        List<AnnotationDefinition> types = new ArrayList<AnnotationDefinition>();
        for (AnnotationDefinition type : annotationDefinitions.values()) {
            if (filter.accept(type)) {
                types.add(type);
            }
        }
        return types;
    }

    public Map<String, AnnotationDefinition> getAnnotationDefinitionsMap() {
        return annotationDefinitions;
    }

    public AnnotationDefinition getAnnotationDefinition(String name) {
        AnnotationDefinition def = annotationDefinitions.get(name);
        return def != null ? def : getFirsTannotationDefinition();
    }

    private AnnotationDefinition getFirsTannotationDefinition() {
        List<AnnotationDefinition> l = new ArrayList<AnnotationDefinition>(
                annotationDefinitions.values());
        return l.isEmpty() ? null : l.get(0);
    }

    public void setUserInfo(Map<String, String> userInfo) {
        this.userInfo = userInfo;
    }

    public Map<String, String> getUserInfo() {
        return userInfo;
    }

    private String getValue(String v) {
        if (v != null && v.startsWith("${") && v.endsWith("}")) {
            v = userInfo.get(v.substring(2, v.length() - 1));
        }
        return v;
    }

    public void addFilter(int order, String name, String icon, String type,
            String author, Map<String, String> fields) {
        Map<String, String> newFields = new HashMap<String, String>();
        for (String fieldName : fields.keySet()) {
            String value = getValue(fields.get(fieldName));
            if (value != null) {
                newFields.put(fieldName, value);
            }
        }
        if (order < filters.size()) {
            filters.add(order, new AnnotationFilter(name, icon, getValue(type),
                    getValue(author), newFields));
        } else {
            filters.add(new AnnotationFilter(name, icon, getValue(type),
                    getValue(author), newFields));
        }
    }

    public List<AnnotationFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<AnnotationFilter> filters) {
        this.filters = filters;
    }

    public Set<String> getDisplayedFields() {
        return displayedFields;
    }

    public void setDisplayedFields(Set<String> fields) {
        this.displayedFields = fields;
    }

    public void setFieldLabels(Map<String, String> fieldLabels) {
        this.fieldLabels = fieldLabels;
    }

    public Map<String, String> getFieldLabels() {
        return fieldLabels;
    }

    public void setCanAnnotate(boolean canAnnotate) {
        this.canAnnotate = canAnnotate;
    }

    public boolean canAnnotate() {
        return canAnnotate;
    }

}
