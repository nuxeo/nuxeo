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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class Service extends DefaultComponent {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.theme.webwidgets.Service");

    private static final Log log = LogFactory.getLog(Service.class);

    private Map<String, WidgetType> widgetTypes;

    private Set<String> widgetCategories;

    private Map<String, ProviderType> providerTypes;

    private Map<String, DecorationType> decorationTypes;

    @Override
    public void activate(ComponentContext context) {
        widgetTypes = new LinkedHashMap<String, WidgetType>();
        widgetCategories = new HashSet<String>();
        providerTypes = new LinkedHashMap<String, ProviderType>();
        decorationTypes = new LinkedHashMap<String, DecorationType>();
        log.debug("Web widgets service activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        widgetTypes = null;
        widgetCategories = null;
        providerTypes = null;
        decorationTypes = null;
        log.debug("Web widgets service deactivated");
    }

    @Override
    public void registerExtension(Extension extension) {
        final String xp = extension.getExtensionPoint();
        if (xp.equals("widgets")) {
            registerWidget(extension);
        } else if (xp.equals("providers")) {
            registerProvider(extension);
        } else if (xp.equals("decorations")) {
            registerDecoration(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        final String xp = extension.getExtensionPoint();
        if (xp.equals("widgets")) {
            unregisterWidget(extension);
        } else if (xp.equals("providers")) {
            unregisterProvider(extension);
        } else if (xp.equals("decorations")) {
            unregisterDecoration(extension);
        }
    }

    private void registerWidget(Extension extension) {
        final Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            WidgetType widgetType = (WidgetType) contrib;
            initializeWidget(widgetType);
            final String name = widgetType.getName();
            widgetTypes.put(name, widgetType);
            widgetCategories.add(widgetType.getCategory());
        }
    }

    private void registerProvider(Extension extension) {
        final Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ProviderType providerType = (ProviderType) contrib;
            final String providerName = providerType.getName();
            providerTypes.put(providerName, providerType);
        }
    }

    private void registerDecoration(Extension extension) {
        final Object[] contribs = extension.getContributions();
        final String[] modes = { "view", "edit" };
        for (Object contrib : contribs) {
            DecorationType decorationType = (DecorationType) contrib;
            final String decorationName = decorationType.getName();
            decorationTypes.put(decorationName, decorationType);
            for (String mode : modes) {
                if (decorationType.getPanelDecoration(mode) == null) {
                    log.warn("Widget decoration '" + decorationName
                            + "' has no panel decoration in '" + mode
                            + "' mode");
                }
                if (decorationType.getWidgetDecoration(mode) == null) {
                    log.warn("Widget decoration '" + decorationName
                            + "' has no widget decoration in '" + mode
                            + "' mode");
                }
            }
        }
    }

    private void unregisterWidget(Extension extension) {
        final Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            WidgetType widgetType = (WidgetType) contrib;
            widgetTypes.remove(widgetType.getName());
            widgetCategories.remove(widgetType.getCategory());
        }
    }

    private void unregisterProvider(Extension extension) {
        final Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ProviderType providerType = (ProviderType) contrib;
            providerTypes.remove(providerType.getName());
        }
    }

    private void unregisterDecoration(Extension extension) {
        final Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            DecorationType decorationType = (DecorationType) contrib;
            decorationTypes.remove(decorationType.getName());
        }
    }

    /*
     * API
     */
    public List<String> getProviderNames() {
        return new ArrayList<String>(providerTypes.keySet());
    }

    public List<String> getDecorationNames() {
        return new ArrayList<String>(decorationTypes.keySet());
    }

    public List<String> getWidgetTypeNames() {
        return new ArrayList<String>(widgetTypes.keySet());
    }

    public List<WidgetType> getWidgetTypes(String category) {
        final List<WidgetType> types = new ArrayList<WidgetType>();
        for (Map.Entry<String, WidgetType> entry : widgetTypes.entrySet()) {
            final WidgetType widgetType = entry.getValue();
            if (category.equals("")
                    || widgetType.getCategory().equals(category)) {
                types.add(widgetType);
            }
        }
        return types;
    }

    public WidgetType getWidgetType(String widgetTypeName) {
        return widgetTypes.get(widgetTypeName);
    }

    private void initializeWidget(WidgetType widgetType) {
        final String path = widgetType.getPath();
        final String source = Utils.readResource(path);
        widgetType.setSource(source);
        final String icon = Utils.extractIcon(source);
        if (icon != null) {
            widgetType.setIcon(icon);
        }
        widgetType.setAuthor(Utils.extractMetadata(source, "author"));
        widgetType.setDescription(Utils.extractMetadata(source, "description"));
        widgetType.setThumbnail(Utils.extractMetadata(source, "thumbnail"));
        widgetType.setScreenshot(Utils.extractMetadata(source, "screenshot"));
        widgetType.setWebsite(Utils.extractMetadata(source, "website"));

        widgetType.setSchema(Utils.extractSchema(source));
        widgetType.setScripts(Utils.extractScripts(source));
        widgetType.setStyles(Utils.extractStyles(source));
        widgetType.setBody(Utils.extractBody(source));
    }

    public Set<String> getWidgetCategories() {
        return widgetCategories;
    }

    public ProviderType getProviderType(String name) {
        return providerTypes.get(name);
    }

    public DecorationType getDecorationType(String name) {
        return decorationTypes.get(name);
    }

    public String getPanelDecoration(String decorationName, String mode) {
        DecorationType decorationType = getDecorationType(decorationName);
        PanelDecorationType panelDecorationType = decorationType.getPanelDecoration(mode);
        if (panelDecorationType == null) {
            return "";
        }
        return panelDecorationType.getContent();
    }

}
