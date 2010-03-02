/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client.bean;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * GadgetBean
 * 
 * @author Guillaume Cusnieux
 */
public class GadgetBean implements Comparable<GadgetBean>, IsSerializable {

    private static final long serialVersionUID = 1L;

    private String ref;

    private String title;

    private String renderUrl;

    private String viewer;

    private List<PreferencesBean> defaultPrefs;

    private List<PreferencesBean> userPrefs;

    private List<String> permissions;

    private GadgetPosition position;

    private boolean collapsed;

    private String name;

    private String icon;

    private Map<String, GadgetView> gadgetViews;

    private String htmlContent;

    private Integer height;

    /**
     * Default construcor (Specification of Gwt)
     */
    public GadgetBean() {
    }

    public GadgetBean(String ref, String title, String viewer,
            List<PreferencesBean> defaultPrefs,
            List<PreferencesBean> userPrefs, List<String> permissions,
            boolean collapsed, String name,
            Map<String, GadgetView> gadgetViews, String htmlContent,
            Integer height) {
        this.ref = ref;
        this.title = title;
        this.defaultPrefs = defaultPrefs;
        this.userPrefs = userPrefs;
        this.viewer = viewer;
        this.permissions = permissions;
        this.collapsed = collapsed;
        this.name = name;
        this.gadgetViews = gadgetViews;
        this.htmlContent = htmlContent;
        this.height = height;
    }

    public void setRenderUrl(String renderUrl) {
        this.renderUrl = renderUrl;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, GadgetView> getGadgetViews() {
        return gadgetViews;
    }

    public void setGadgetViews(Map<String, GadgetView> gadgetViews) {
        this.gadgetViews = gadgetViews;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public GadgetPosition getPosition() {
        return position;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setViewer(String viewer) {
        this.viewer = viewer;
    }

    public void setDefaultPrefs(List<PreferencesBean> defaultPrefs) {
        this.defaultPrefs = defaultPrefs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getRef() {
        return ref;
    }

    public String getRenderUrl() {
        return renderUrl;
    }

    public List<PreferencesBean> getDefaultPrefs() {
        return defaultPrefs;
    }

    public List<PreferencesBean> getUserPrefs() {
        return userPrefs;
    }

    public void setUserPrefs(List<PreferencesBean> prefs) {
        userPrefs = prefs;
    }

    public String getViewer() {
        return viewer;
    }

    public boolean hasPermission(String name) {
        return permissions.contains(name);
    }

    public void setPermission(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void setPosition(GadgetPosition position) {
        this.position = position;
    }

    public GadgetPosition getGadgetPosition() {
        return position;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public GadgetView getView(String view) {
        return gadgetViews.get(view);
    }

    public void setPref(String key, String value) {
        for (PreferencesBean pref : this.userPrefs) {
            if (key.equals(pref.getName())) {
                pref.setValue(value);
                return;
            }
        }

        for (PreferencesBean pref : this.defaultPrefs) {
            if (key.equals(pref.getName())) {
                pref.setValue(value);
                return;
            }
        }

    }

    public int compareTo(GadgetBean o) {
        Integer pos1 = o.getGadgetPosition().getPosition();
        Integer pos2 = this.getGadgetPosition().getPosition();

        return pos2 - pos1;

    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

}
