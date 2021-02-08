/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;

@XObject("type")
@XRegistry
public class Type {

    public static final String[] EMPTY_ACTIONS = new String[0];

    @XNode("@id")
    @XRegistryId
    protected String id;

    @XNode("icon")
    protected String icon;

    @XNode("icon-expanded")
    protected String iconExpanded;

    @XNode("bigIcon")
    protected String bigIcon;

    @XNode("bigIcon-expanded")
    protected String bigIconExpanded;

    @XNode("label")
    protected String label;

    @XNodeList(value = "subtypes/type", type = ArrayList.class, componentType = SubType.class)
    @XMerge(defaultAssignment = false) // compat
    protected List<SubType> allowedSubTypes = new ArrayList<>();

    @XNodeList(value = "deniedSubtypes/type", type = String[].class, componentType = String.class)
    @XMerge(defaultAssignment = false) // for consistency with subtypes
    protected String[] deniedSubTypes;

    @XNode("default-view")
    protected String defaultView;

    @XNode("create-view")
    protected String createView;

    @XNode("edit-view")
    protected String editView;

    @XNode("description")
    protected String description;

    @XNode("category")
    protected String category;

    @XNodeMap(value = "views/view", key = "@id", type = HashMap.class, componentType = TypeView.class)
    protected Map<String, TypeView> views;

    @XNodeList(value = "actions/action", type = String[].class, componentType = String.class)
    @XMerge(defaultAssignment = false) // compat
    protected String[] actions;

    @XNodeMap(value = "layouts", key = "@mode", type = HashMap.class, componentType = Layouts.class)
    @XMerge(value = "layouts@append", defaultAssignment = false) // compat
    protected Map<String, Layouts> layouts;

    @XNodeMap(value = "contentViews", key = "@category", type = HashMap.class, componentType = DocumentContentViews.class)
    @XMerge(value = "contentViews@append", defaultAssignment = false) // compat
    protected Map<String, DocumentContentViews> contentViews;

    // needed by xmap
    public Type() {
    }

    // helper for tests
    public Type(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String[] getActions() {
        return actions;
    }

    public String getIcon() {
        return icon;
    }

    public String getBigIcon() {
        return bigIcon;
    }

    public String getBigIconExpanded() {
        return bigIconExpanded;
    }

    public String getLabel() {
        return label;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    /**
     * Returns layout names given a mode.
     */
    public String[] getLayouts(String mode) {
        // default to mode ANY
        return getLayouts(mode, BuiltinModes.ANY);
    }

    public String[] getLayouts(String mode, String defaultMode) {
        Layouts modeLayouts = layouts.get(mode);
        if (modeLayouts == null && defaultMode != null) {
            modeLayouts = layouts.get(defaultMode);
        }
        if (modeLayouts != null) {
            return modeLayouts.getLayouts();
        }
        return new String[0];
    }

    /**
     * Returns the layouts map
     */
    public Map<String, Layouts> getLayouts() {
        return Collections.unmodifiableMap(layouts);
    }

    public String getDefaultView() {
        return defaultView;
    }

    public String getCreateView() {
        return createView;
    }

    public String getEditView() {
        return editView;
    }

    public TypeView[] getViews() {
        return views.values().toArray(new TypeView[views.size()]);
    }

    public TypeView getView(String viewId) {
        return views.get(viewId);
    }

    public String[] getDeniedSubTypes() {
        return deniedSubTypes;
    }

    public void setDeniedSubTypes(String[] deniedSubTypes) {
        this.deniedSubTypes = deniedSubTypes;
    }

    public Map<String, SubType> getAllowedSubTypes() {
        Map<String, SubType> map = new HashMap<>();
        for (SubType type : allowedSubTypes) {
            String name = type.getName();
            SubType previousType = map.get(name);
            if (previousType == null) {
                map.put(name, type);
            } else {
                // merge multiple declarations
                Set<String> hiddenMerged = new HashSet<>();
                hiddenMerged.addAll(previousType.getHidden());
                hiddenMerged.addAll(type.getHidden());
                map.put(name, new SubType(name, hiddenMerged));
            }
        }
        return map;
    }

    public void setAllowedSubTypes(Map<String, SubType> allowedSubTypes) {
        this.allowedSubTypes = new ArrayList<>(allowedSubTypes.values());
    }

    public String getIconExpanded() {
        return iconExpanded;
    }

    /**
     * Return content views defined on this document type for given category
     *
     * @since 5.4
     */
    public String[] getContentViews(String category) {
        if (contentViews != null) {
            DocumentContentViews cv = contentViews.get(category);
            if (cv != null) {
                return cv.getContentViewNames();
            }
        }
        return new String[0];
    }

    public Map<String, DocumentContentViews> getContentViews() {
        return Collections.unmodifiableMap(contentViews);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Type.class.getSimpleName());
        sb.append(" {");
        sb.append("id: ");
        sb.append(id);
        sb.append('}');
        return sb.toString();
    }

}
