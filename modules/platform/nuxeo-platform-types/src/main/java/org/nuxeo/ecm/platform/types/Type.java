/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;

@XObject("type")
public class Type {

    public static final String[] EMPTY_ACTIONS = new String[0];

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

    protected Type(String id) {
        this.id = id;
    }

    public Type() {

    }

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

    @XNodeMap(value = "layouts", key = "@mode", type = HashMap.class, componentType = Layouts.class)
    Map<String, Layouts> layouts;

    @XNodeMap(value = "contentViews", key = "@category", type = HashMap.class, componentType = DocumentContentViews.class)
    protected Map<String, DocumentContentViews> contentViews;

    // for bundle update::
    @XNode("@remove")
    protected boolean remove = false;

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

    /**
     * Returns layout names given a mode.
     */
    public String[] getLayouts(String mode) {
        // default to mode ANY
        return getLayouts(mode, BuiltinModes.ANY);
    }

    public String[] getLayouts(String mode, String defaultMode) {
        if (layouts != null) {
            Layouts layouts = this.layouts.get(mode);
            if (layouts == null && defaultMode != null) {
                layouts = this.layouts.get(defaultMode);
            }
            if (layouts != null) {
                return layouts.getLayouts();
            }
        }
        return null;
    }

    /**
     * Returns the layouts map
     */
    public Map<String, Layouts> getLayouts() {
        return Collections.unmodifiableMap(layouts);
    }

    public void setLayouts(Map<String, Layouts> layouts) {
        this.layouts = layouts;
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
        return views.values().toArray(new TypeView[views.size()]);
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

    /**
     * Clone method to handle hot reload
     *
     * @since 5.6
     */
    @Override
    public Type clone() {
        Type clone = new Type();
        clone.setId(getId());
        clone.setIcon(getIcon());
        clone.setIconExpanded(getIconExpanded());
        clone.setBigIcon(getBigIcon());
        clone.setBigIconExpanded(getBigIconExpanded());
        clone.setLabel(getLabel());
        Map<String, SubType> subs = getAllowedSubTypes();
        if (subs != null) {
            Map<String, SubType> csubs = new HashMap<>();
            for (Map.Entry<String, SubType> item : subs.entrySet()) {
                csubs.put(item.getKey(), item.getValue().clone());
            }
            clone.setAllowedSubTypes(csubs);
        }
        String[] denied = getDeniedSubTypes();
        if (denied != null) {
            clone.setDeniedSubTypes(denied.clone());
        }
        clone.setDefaultView(getDefaultView());
        clone.setCreateView(getCreateView());
        clone.setEditView(getEditView());
        clone.setDescription(getDescription());
        clone.setCategory(getCategory());
        if (views != null) {
            Map<String, TypeView> cviews = new HashMap<>();
            for (Map.Entry<String, TypeView> item : views.entrySet()) {
                cviews.put(item.getKey(), item.getValue().clone());
            }
            clone.views = cviews;
        }
        String[] actions = getActions();
        if (actions != null) {
            clone.setActions(actions.clone());
        }
        // do not clone old layout definition, nobody's using it anymore
        Map<String, Layouts> layouts = getLayouts();
        if (layouts != null) {
            Map<String, Layouts> clayouts = new HashMap<>();
            for (Map.Entry<String, Layouts> item : layouts.entrySet()) {
                clayouts.put(item.getKey(), item.getValue().clone());
            }
            clone.setLayouts(clayouts);
        }
        Map<String, DocumentContentViews> cvs = getContentViews();
        if (cvs != null) {
            Map<String, DocumentContentViews> ccvs = new HashMap<>();
            for (Map.Entry<String, DocumentContentViews> item : cvs.entrySet()) {
                ccvs.put(item.getKey(), item.getValue().clone());
            }
            clone.setContentViews(ccvs);
        }
        clone.setRemove(getRemove());
        return clone;
    }

}
