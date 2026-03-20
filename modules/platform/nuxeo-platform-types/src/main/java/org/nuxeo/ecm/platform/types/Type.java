/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.types;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("type")
public class Type implements Descriptor {

    @XNode("@id")
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

    protected Map<String, SubType> allowedSubTypes = new HashMap<>();

    @XNodeList(value = "subtypes/type", type = ArrayList.class, componentType = SubType.class)
    public void addSubType(List<SubType> subTypes) {
        if (allowedSubTypes == null) {
            allowedSubTypes = new HashMap<>();
        }

        for (SubType currentSubType : subTypes) {
            SubType subTypeToMerge = allowedSubTypes.get(currentSubType.name);
            if (subTypeToMerge == null) {
                allowedSubTypes.put(currentSubType.name, currentSubType);
            } else {
                List<String> currentSubTypeHidden = currentSubType.getHidden();
                List<String> subTypeToMergeHidden = subTypeToMerge.getHidden();
                for (String hidden : currentSubTypeHidden) {
                    if (!subTypeToMergeHidden.contains(hidden)) {
                        subTypeToMergeHidden.add(hidden);
                    }
                }
            }
        }
    }

    @XNodeList(value = "deniedSubtypes/type", type = String[].class, componentType = String.class)
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

    protected Map<String, TypeView> views;

    @XNodeList(value = "actions/action", type = String[].class, componentType = String.class)
    protected String[] actions;

    @XNodeMap(value = "contentViews", key = "@category", type = HashMap.class, componentType = DocumentContentViews.class)
    protected Map<String, DocumentContentViews> contentViews;

    // for bundle update::
    @XNode("@remove")
    protected boolean remove = false;

    public Type() {
    }

    protected Type(String id) {
        this.id = id;
    }

    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getBigIcon() {
        return bigIcon;
    }

    public void setBigIcon(String bigIcon) {
        this.bigIcon = bigIcon;
    }

    public String getBigIconExpanded() {
        return bigIconExpanded;
    }

    public void setBigIconExpanded(String bigIconExpanded) {
        this.bigIconExpanded = bigIconExpanded;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public String getCreateView() {
        return createView;
    }

    public void setCreateView(String createView) {
        this.createView = createView;
    }

    public String getEditView() {
        return editView;
    }

    public void setEditView(String editView) {
        this.editView = editView;
    }

    public TypeView[] getViews() {
        return views.values().toArray(TypeView[]::new);
    }

    @XNodeList(value = "views/view", type = TypeView[].class, componentType = TypeView.class)
    public void setViews(TypeView[] views) {
        this.views = new HashMap<>();
        for (TypeView view : views) {
            this.views.put(view.getId(), view);
        }
    }

    public TypeView getView(String viewId) {
        return views.get(viewId);
    }

    public void setView(TypeView view) {
        views.put(view.getId(), view);
    }

    public String[] getDeniedSubTypes() {
        return deniedSubTypes;
    }

    public void setDeniedSubTypes(String[] deniedSubTypes) {
        this.deniedSubTypes = deniedSubTypes;
    }

    public Map<String, SubType> getAllowedSubTypes() {
        return allowedSubTypes;
    }

    public void setAllowedSubTypes(Map<String, SubType> allowedSubTypes) {
        this.allowedSubTypes = allowedSubTypes;
    }

    public boolean getRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getIconExpanded() {
        return iconExpanded;
    }

    public void setIconExpanded(String iconExpanded) {
        this.iconExpanded = iconExpanded;
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
        return null;
    }

    public Map<String, DocumentContentViews> getContentViews() {
        return Collections.unmodifiableMap(contentViews);
    }

    public void setContentViews(Map<String, DocumentContentViews> contentViews) {
        this.contentViews = contentViews;
    }

    @Override
    public Type merge(Descriptor o) {
        var other = (Type) o;
        var merged = new Type();
        merged.id = getIfNull(other.id, id);
        merged.icon = defaultIfBlank(other.icon, icon);
        merged.iconExpanded = defaultIfBlank(other.iconExpanded, iconExpanded);
        merged.bigIcon = defaultIfBlank(other.bigIcon, bigIcon);
        merged.bigIconExpanded = defaultIfBlank(other.bigIconExpanded, bigIconExpanded);
        merged.label = defaultIfBlank(other.label, label);
        merged.description = defaultIfBlank(other.description, description);
        merged.category = defaultIfBlank(other.category, category);
        // merge allowedSubTypes
        merged.allowedSubTypes = new HashMap<>(allowedSubTypes);
        merged.allowedSubTypes.putAll(other.allowedSubTypes);
        merged.allowedSubTypes.keySet().removeIf(Set.of(nullToEmpty(other.deniedSubTypes))::contains);

        merged.defaultView = defaultIfBlank(other.defaultView, defaultView);
        merged.createView = defaultIfBlank(other.createView, createView);
        merged.editView = defaultIfBlank(other.editView, editView);
        // merge views
        merged.views = new HashMap<>(getIfNull(views, Map.of()));
        merged.views.putAll(getIfNull(other.views, Map.of()));

        merged.actions = isNotEmpty(other.actions) ? other.actions : nullToEmpty(actions);
        // merge contentViews
        merged.contentViews = new HashMap<>(getIfNull(contentViews, Map.of()));
        for (var entry : getIfNull(other.contentViews, Map.<String, DocumentContentViews> of()).entrySet()) {
            if (merged.contentViews.containsKey(entry.getKey()) && entry.getValue().getAppend()) {
                merged.contentViews.merge(entry.getKey(), entry.getValue(), (inLayouts, otherInLayouts) -> {
                    var mergedContentViews = new DocumentContentViews();
                    mergedContentViews.contentViews = addAll(inLayouts.contentViews, otherInLayouts.contentViews);
                    return mergedContentViews;
                });
            } else {
                merged.contentViews.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    @Override
    public boolean doesRemove() {
        return getRemove();
    }

    @Override
    public String toString() {
        return Type.class.getSimpleName() + " {id: " + id + '}';
    }
}
