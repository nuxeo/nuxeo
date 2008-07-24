/*
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets;

import static org.jboss.seam.ScopeType.SESSION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.model.SelectItem;
import javax.interceptor.Interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ejb.SeamInterceptor;
import org.nuxeo.runtime.api.Framework;

@Stateful
@Name("nxthemesWebWidgetManager")
@Scope(SESSION)
@Interceptors(SeamInterceptor.class)
public class Manager implements ManagerLocal {

    private static final Log log = LogFactory.getLog(Manager.class);

    public String widgetCategory = "";

    private static Service getService() {
        return (Service) Framework.getRuntime().getComponent(Service.ID);
    }

    public static List<String> getProviderNames() {
        return getService().getProviderNames();
    }

    public static Provider getProvider(String name) {
        ProviderType providerType = getProviderType(name);
        if (providerType == null) {
            log.error("Provider unknown: " + name);
            return null;
        }
        String componentName = providerType.getComponentName();
        return (Provider) Component.getInstance(componentName, true);
    }

    public static ProviderType getProviderType(String name) {
        return getService().getProviderType(name);
    }

    public static List<String> getDecorationNames() {
        return getService().getDecorationNames();
    }

    public static DecorationType getDecorationType(String name) {
        return getService().getDecorationType(name);
    }

    public static WidgetType getWidgetType(String widgetTypeName) {
        return getService().getWidgetType(widgetTypeName);
    }

    public List<WidgetType> getAvailableWidgetTypes() {
        return getService().getWidgetTypes(widgetCategory);
    }

    public List<SelectItem> getAvailableWidgetCategories() {
        final List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (String category : getService().getWidgetCategories()) {
            selectItemList.add(new SelectItem(category));
        }
        return selectItemList;
    }

    public String getWidgetCategory() {
        return widgetCategory;
    }

    public static String addPanelDecoration(String decorationName, String mode,
            String regionName, String content) {
        String html = getService().getPanelDecoration(decorationName, mode);
        html = html.replaceAll("%REGION_NAME%", regionName);
        html = html.replaceAll("%REGION_BODY%", content);
        return html.trim();
    }

    public void setWidgetCategory(String widgetCategory) {
        this.widgetCategory = widgetCategory;
    }

    public void addWidget(String providerName, String widgetTypeName,
            String region, int order) {
        Provider provider = getProvider(providerName);
        if (!provider.canWrite()) {
            return;
        }
        Widget widget = provider.createWidget(widgetTypeName);
        provider.addWidget(widget, region, order);
    }

    public String moveWidget(String srcProviderName, String destProviderName,
            String srcUid, String srcRegionName, String destRegionName,
            int destOrder) {
        Provider srcProvider = getProvider(srcProviderName);
        Provider destProvider = getProvider(destProviderName);
        if (!srcProvider.canWrite() || !destProvider.canWrite()) {
            return null;
        }
        Widget srcWidget = srcProvider.getWidgetByUid(srcUid);

        String newId = srcWidget.getUid();

        // The widget is moved inside the same region
        if (destRegionName.equals(srcRegionName)) {
            srcProvider.reorderWidget(srcWidget, destOrder);
        } else {
            // The destination provider is the same as the source provider
            if (destProviderName.equals(srcProviderName)) {
                srcProvider.moveWidget(srcWidget, destRegionName, destOrder);
                // The destination provider is different from the source
                // provider, the widget must be duplicated.
            } else {
                Widget destWidget = destProvider.createWidget(srcWidget.getName());
                setWidgetPreferences(destProvider, destWidget,
                        getWidgetPreferences(srcProvider, srcWidget));
                setWidgetState(destProvider, destWidget, getWidgetState(
                        srcProvider, srcWidget));
                srcProvider.removeWidget(srcWidget);
                destProvider.addWidget(destWidget, destRegionName, destOrder);
                newId = destWidget.getUid();
            }
        }
        return newId;
    }

    public void removeWidget(String providerName, String uid) {
        Provider provider = getProvider(providerName);
        if (!provider.canWrite()) {
            return;
        }
        Widget widget = provider.getWidgetByUid(uid);
        provider.deleteWidgetData(widget);
        provider.removeWidget(widget);
    }

    /*
     * Widget preferences
     */
    public static Map<String, String> getWidgetPreferences(Provider provider,
            Widget widget) {
        Map<String, String> widgetPreferences = new HashMap<String, String>();
        if (provider.canRead()) {
            Map<String, String> preferences = provider.getWidgetPreferences(widget);
            if (preferences != null) {
                widgetPreferences.putAll(preferences);
            }

            final String widgetTypeName = widget.getName();
            WidgetType widgetType = getWidgetType(widgetTypeName);

            for (WidgetFieldType fieldType : widgetType.getSchema()) {
                final String name = fieldType.getName();
                if (widgetPreferences.get(name) == null) {
                    widgetPreferences.put(name, fieldType.getDefaultValue());
                }
            }
        }
        return widgetPreferences;
    }

    public static void setWidgetPreferences(Provider provider, Widget widget,
            Map<String, String> preferences) {
        if (!provider.canWrite()) {
            return;
        }
        if (preferences == null) {
            preferences = new HashMap<String, String>();
        }
        provider.setWidgetPreferences(widget, preferences);
    }

    public void updateWidgetPreferences(String providerName, String uid,
            Map<String, String> preferences) {
        Provider provider = getProvider(providerName);
        if (!provider.canWrite()) {
            return;
        }
        Widget widget = provider.getWidgetByUid(uid);
        Map<String, String> newPreferences = new HashMap<String, String>();
        Map<String, String> oldPreferences = provider.getWidgetPreferences(widget);
        if (oldPreferences != null) {
            newPreferences.putAll(oldPreferences);
        }
        newPreferences.putAll(preferences);
        provider.setWidgetPreferences(widget, newPreferences);
    }

    /*
     * Widget state
     */
    public void setWidgetState(Provider provider, Widget widget,
            WidgetState state) {
        if (!provider.canWrite()) {
            return;
        }
        if (state == null) {
            state = WidgetState.DEFAULT;
        }
        provider.setWidgetState(widget, state);
    }

    public void setWidgetState(String providerName, String uid, String stateName) {
        Provider provider = getProvider(providerName);
        if (!provider.canWrite()) {
            return;
        }
        Widget widget = provider.getWidgetByUid(uid);
        for (WidgetState state : WidgetState.values()) {
            if (state.getName().equals(stateName)) {
                provider.setWidgetState(widget, state);
                return;
            }
        }
        provider.setWidgetState(widget, WidgetState.DEFAULT);
    }

    public static WidgetState getWidgetState(Provider provider, Widget widget) {
        if (provider.canRead()) {
            WidgetState state = provider.getWidgetState(widget);
            if (state != null) {
                return state;
            }
        }
        return WidgetState.DEFAULT;
    }

    /*
     * Widget data
     */

    public void setWidgetData(String providerName, String uid, String dataName,
            WidgetData data) {
        final Provider provider = getProvider(providerName);
        final Widget widget = provider.getWidgetByUid(uid);
        provider.setWidgetData(widget, dataName, data);
    }

    public void setWidgetData(Provider provider, Widget widget,
            String dataName, WidgetData data) {
        if (provider.canWrite()) {
            provider.setWidgetData(widget, dataName, data);
        }
    }

    public WidgetData getWidgetData(String providerName, String uid,
            String dataName) {
        final Provider provider = getProvider(providerName);
        if (!provider.canRead()) {
            return null;
        }
        final Widget widget = provider.getWidgetByUid(uid);
        return getWidgetData(provider, widget, dataName);
    }

    public WidgetData getWidgetData(Provider provider, Widget widget,
            String dataName) {
        if (provider.canRead()) {
            final WidgetData data = provider.getWidgetData(widget, dataName);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    public String getWidgetDataInfo(String providerName, String uid,
            String dataName) {
        final WidgetData data = getWidgetData(providerName, uid, dataName);
        if (data != null) {
            final Map<String, String> fileInfo = new HashMap<String, String>();
            fileInfo.put("content-type", data.getContentType());
            fileInfo.put("filename", data.getFilename());
            return org.nuxeo.theme.html.Utils.toJson(fileInfo);
        } else {
            return null;
        }
    }

    /*
     * UI
     */
    public String getPanelData(String providerName, String regionName,
            String mode) {
        final Provider provider = getProvider(providerName);
        final Map<String, Object> data = new HashMap<String, Object>();
        final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        final Map<String, Map<String, Object>> types = new HashMap<String, Map<String, Object>>();

        if (provider != null) {
            final List<Widget> widgets = provider.getWidgets(regionName);
            final Set<String> widgetTypeNames = new HashSet<String>();
            if (widgets != null) {
                for (Widget widget : widgets) {
                    widgetTypeNames.add(widget.getName());
                }

                for (String widgetTypeName : widgetTypeNames) {
                    WidgetType widgetType = getWidgetType(widgetTypeName);
                    types.put(widgetTypeName, widgetType.getInfo());
                }

                if (provider.canRead()) {
                    for (Widget widget : widgets) {
                        final Map<String, Object> item = new HashMap<String, Object>();
                        item.put("uid", widget.getUid());
                        item.put("name", widget.getName());
                        item.put("preferences", getWidgetPreferences(provider,
                                widget));
                        item.put("state",
                                getWidgetState(provider, widget).getName());
                        items.add(item);
                    }
                }
            }

        } else {
            log.error("Provider not found: " + providerName);
        }

        // Switch to default read mode if the user does not have write access.
        if (!provider.canWrite()) {
            mode = "*";
        }
        data.put("mode", mode);
        data.put("widget_types", types);
        data.put("widget_items", items);
        return org.nuxeo.theme.html.Utils.toJson(data);
    }

    @Remove
    @Destroy
    public void destroy() {
    }

}
