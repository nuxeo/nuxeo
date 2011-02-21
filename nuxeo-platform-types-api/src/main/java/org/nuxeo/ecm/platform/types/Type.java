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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;

@XObject("type")
public class Type implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(Type.class);

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

    protected Map<String, SubType> allowedSubTypes;

    @XNodeList(value = "subtypes/type", type = ArrayList.class, componentType = SubType.class)
    public void addSubType(List<SubType> subTypes) {
        if (allowedSubTypes == null) {
            allowedSubTypes = new HashMap<String, SubType>();
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

    @XNodeList(value = "layout/widget", type = FieldWidget[].class, componentType = FieldWidget.class)
    protected FieldWidget[] layout;

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
     * @deprecated use {@link #getLayouts(String)} instead, using mode
     *             {@link BuiltinModes#ANY}.
     */
    @Deprecated
    public FieldWidget[] getLayout() {
        return layout;
    }

    @Deprecated
    public void setLayout(FieldWidget[] layout) {
        this.layout = layout;
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
        this.views = new HashMap<String, TypeView>();
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
        final StringBuilder buf = new StringBuilder();
        buf.append(Type.class.getSimpleName());
        buf.append(" {");
        buf.append("id: ");
        buf.append(id);
        buf.append('}');
        return buf.toString();
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

}
