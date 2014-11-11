/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.themes.ThemeManager;

public class Manager {

    private static final Log log = LogFactory.getLog(Manager.class);

    // Utility class.
    private Manager() {
    }

    public static Service getService() {
        return (Service) Framework.getRuntime().getComponent(Service.ID);
    }

    public static List<String> getProviderNames() {
        return getService().getProviderNames();
    }

    public static Provider getProvider(String name) throws WidgetException {
        return getService().getProvider(name);
    }

    public static AreaFragment getAreaById(int area) throws WidgetException {
        org.nuxeo.theme.Manager.getThemeManager();
        Element element = ThemeManager.getElementById(area);
        if (!(element instanceof AreaFragment)) {
            throw new WidgetException("Could not find area fragment");
        }
        return (AreaFragment) element;
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

    public static Map<String, String> getWidgetDecoration(String decorationName) {
        final Map<String, String> data = new HashMap<String, String>();
        final DecorationType decorationType = Manager.getDecorationType(decorationName);
        if (decorationType != null) {
            for (String mode : decorationType.getWindowDecorationModes()) {
                data.put(mode,
                        decorationType.getWidgetDecoration(mode).getContent());
            }
        } else {
            log.error("Decoration not found: " + decorationName);
        }
        return data;
    }

    public static String addPanelDecoration(String decorationName, String mode,
            String regionName, String content) {
        String html = getService().getPanelDecoration(decorationName, mode);
        html = html.replaceAll("%REGION_NAME%", regionName);
        html = html.replaceAll("%REGION_BODY%", content);
        return html.trim();
    }

    public static void addWidget(int area, String widgetTypeName, int order)
            throws WidgetException, ProviderException {
        AreaFragment areaFragment = getAreaById(area);

        String providerName = areaFragment.getProviderName();
        Provider provider = getProvider(providerName);
        if (!provider.canWrite()) {
            throw new WidgetException("No permission to add widget into: "
                    + providerName);
        }

        List<Widget> widgets = provider.getWidgets(areaFragment.getRegionName());

        Integer maxItems = areaFragment.getMaxItems();
        if (maxItems != null && maxItems > 0) {
            if (widgets.size() >= maxItems) {
                throw new WidgetException(String.format(
                        "At most %s widget(s) are allowed.", maxItems));
            }
        }

        Boolean disallowDuplicates = areaFragment.getDisallowDuplicates();
        if (Boolean.TRUE.equals(disallowDuplicates)) {
            for (Widget w : widgets) {
                if (widgetTypeName.equals(w.getName())) {
                    throw new WidgetException(String.format(
                            "Only one '%s' widget is allowed.", widgetTypeName));
                }
            }
        }

        Widget widget = provider.createWidget(widgetTypeName);
        provider.addWidget(widget, areaFragment.getRegionName(), order);
    }

    public static String moveWidget(int srcArea, String srcUid, int destArea,
            int destOrder) throws WidgetException, ProviderException {
        AreaFragment srcAreaFragment = getAreaById(srcArea);
        AreaFragment destAreaFragment = getAreaById(destArea);
        String srcProviderName = srcAreaFragment.getProviderName();
        String destProviderName = destAreaFragment.getProviderName();
        String srcRegionName = srcAreaFragment.getRegionName();
        String destRegionName = destAreaFragment.getRegionName();

        Provider srcProvider = getProvider(srcProviderName);
        Provider destProvider = getProvider(destProviderName);
        if (!srcProvider.canWrite() || !destProvider.canWrite()) {
            throw new WidgetException("No permission to move widget.");
        }
        if (srcRegionName == null || destRegionName == null) {
            throw new WidgetException(
                    "Source or destination region is undefined.");
        }

        Widget srcWidget = srcProvider.getWidgetByUid(srcUid);

        if (!destRegionName.equals(srcRegionName)) {
            List<Widget> widgets = destProvider.getWidgets(destAreaFragment.getRegionName());
            Integer maxItems = destAreaFragment.getMaxItems();
            if (maxItems != null) {
                if (widgets.size() >= maxItems) {
                    throw new WidgetException(String.format(
                            "Max %s item(s) are allowed.", maxItems));
                }
            }
            Boolean disallowDuplicates = destAreaFragment.getDisallowDuplicates();
            if (Boolean.TRUE.equals(disallowDuplicates)) {
                String widgetTypeName = srcWidget.getName();
                for (Widget w : widgets) {
                    if (widgetTypeName.equals(w.getName())) {
                        throw new WidgetException(String.format(
                                "Only one '%s' widget is allowed.",
                                widgetTypeName));
                    }
                }
            }
        }

        String newId = srcWidget.getUid();

        // The destination provider is the same as the source provider
        if (destProviderName.equals(srcProviderName)) {
            // The widget is moved inside the same region
            if (destRegionName.equals(srcRegionName)) {
                srcProvider.reorderWidget(srcWidget, destOrder);
            } else {
                srcProvider.moveWidget(srcWidget, destRegionName, destOrder);
            }
            // The destination provider is different from the source
            // provider, the widget must be duplicated.
        } else {
            Widget destWidget = destProvider.createWidget(srcWidget.getName());
            setWidgetPreferences(destProvider, destWidget,
                    getWidgetPreferences(srcProvider, srcWidget));
            setWidgetState(destProvider, destWidget, getWidgetState(
                    srcProvider, srcWidget));
            destProvider.addWidget(destWidget, destRegionName, destOrder);
            srcProvider.removeWidget(srcWidget);
            newId = destWidget.getUid();
        }
        return newId;
    }

    public static void removeWidget(String providerName, String uid)
            throws WidgetException, ProviderException {
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
            Widget widget) throws ProviderException {
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
            Map<String, String> preferences) throws ProviderException {
        if (!provider.canWrite()) {
            return;
        }
        if (preferences == null) {
            preferences = new HashMap<String, String>();
        }
        provider.setWidgetPreferences(widget, preferences);
    }

    public static void updateWidgetPreferences(String providerName, String uid,
            Map<String, String> preferences) throws WidgetException,
            ProviderException {
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

    public static void setWidgetPreference(String providerName, String uid,
            String name, String value) throws WidgetException,
            ProviderException {
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
        newPreferences.put(name, value);
        provider.setWidgetPreferences(widget, newPreferences);
    }

    /*
     * Widget state
     */
    public static void setWidgetState(Provider provider, Widget widget,
            WidgetState state) throws ProviderException {
        if (!provider.canWrite()) {
            return;
        }
        if (state == null) {
            state = WidgetState.DEFAULT;
        }
        provider.setWidgetState(widget, state);
    }

    public static void setWidgetState(String providerName, String uid,
            String stateName) throws WidgetException, ProviderException {
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

    public static WidgetState getWidgetState(Provider provider, Widget widget)
            throws ProviderException {
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

    public static void setWidgetData(String providerName, String uid,
            String dataName, WidgetData data) throws WidgetException,
            ProviderException {
        final Provider provider = getProvider(providerName);
        final Widget widget = provider.getWidgetByUid(uid);
        provider.setWidgetData(widget, dataName, data);
    }

    public static void setWidgetData(Provider provider, Widget widget,
            String dataName, WidgetData data) throws ProviderException {
        if (provider.canWrite()) {
            provider.setWidgetData(widget, dataName, data);
        }
    }

    public static WidgetData getWidgetData(String providerName, String uid,
            String dataName) throws WidgetException, ProviderException {
        final Provider provider = getProvider(providerName);
        if (!provider.canRead()) {
            throw new WidgetException(
                    "Cannot read content of widget provider: " + providerName);
        }
        final Widget widget = provider.getWidgetByUid(uid);
        return getWidgetData(provider, widget, dataName);
    }

    public static WidgetData getWidgetData(Provider provider, Widget widget,
            String dataName) throws ProviderException {
        if (provider.canRead()) {
            final WidgetData data = provider.getWidgetData(widget, dataName);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    public static String uploadFile(HttpServletRequest request,
            String providerName, String uid, String dataName)
            throws WidgetException, ProviderException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<?> fileItems = null;
        try {
            fileItems = upload.parseRequest(request);
        } catch (FileUploadException e) {
            log.error("Could not upload file", e);
        }
        if (fileItems == null) {
            log.error("No file upload found.");
            return "";
        }
        WidgetData data = null;
        Iterator<?> it = fileItems.iterator();
        if (it.hasNext()) {
            FileItem fileItem = (FileItem) it.next();
            if (!fileItem.isFormField()) {
                /* The file item contains an uploaded file */
                final String contentType = fileItem.getContentType();
                final byte[] fileData = fileItem.get();
                final String filename = fileItem.getName();
                data = new WidgetData(contentType, filename, fileData);
            }
        }
        Manager.setWidgetData(providerName, uid, dataName, data);
        return String.format(
                "<script type=\"text/javascript\">window.parent.NXThemesWebWidgets.getUploader('%s', '%s', '%s').complete();</script>",
                providerName, uid, dataName);
    }

    public static String getWidgetDataContent(String providerName, String uid,
            String dataName) throws WidgetException, ProviderException {
        WidgetData data = getWidgetData(providerName, uid, dataName);
        if (data == null) {
            return "";
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            os.write(data.getContent());
        } catch (IOException e) {
            log.error(e);
        }
        return os.toString();
    }

    public static String getWidgetDataInfo(String providerName, String uid,
            String dataName) throws WidgetException, ProviderException {
        final WidgetData data = getWidgetData(providerName, uid, dataName);
        if (data != null) {
            final Map<String, String> fileInfo = new HashMap<String, String>();
            fileInfo.put("content-type", data.getContentType());
            fileInfo.put("filename", data.getFilename());
            return org.nuxeo.theme.html.Utils.toJson(fileInfo);
        }
        return null;
    }

    /*
     * UI
     */
    public static String getPanelData(int area, String mode)
            throws WidgetException, ProviderException {
        final AreaFragment areaFragment = getAreaById(area);
        final String providerName = areaFragment.getProviderName();
        final String regionName = areaFragment.getRegionName();
        final Provider provider = getProvider(providerName);
        final String decoration = areaFragment.getDecoration();
        final Map<String, Object> data = new HashMap<String, Object>();
        final List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        final Map<String, Map<String, Object>> types = new HashMap<String, Map<String, Object>>();

        if (provider == null) {
            throw new WidgetException("Provider not found: " + providerName);
        }

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

        // Switch to default read mode if the user does not have write access.
        if (!provider.canWrite()) {
            mode = "*";
        }
        data.put("mode", mode);
        data.put("provider", providerName);
        data.put("region", regionName);
        data.put("decoration", decoration);
        data.put("widget_decoration", Manager.getWidgetDecoration(decoration));
        data.put("widget_types", types);
        data.put("widget_items", items);
        return org.nuxeo.theme.html.Utils.toJson(data);
    }

    public static String getWidgetIconPath(final String widgetTypeName) {
        WidgetType widgetType = getWidgetType(widgetTypeName);
        String iconResourcePath = null;
        if (widgetType != null) {
            iconResourcePath = widgetType.getIconPath();
        }
        if (iconResourcePath == null) {
            iconResourcePath = "nxthemes/html/icons/no-icon.png";
        }
        return iconResourcePath;
    }

    public static byte[] getWidgetIconContent(final String widgetTypeName) {
        String iconResourcePath = getWidgetIconPath(widgetTypeName);
        try {
            return org.nuxeo.theme.Utils.readResourceAsBytes(iconResourcePath);
        } catch (IOException e) {
            throw new ClientRuntimeException(e);
        }
    }
}
