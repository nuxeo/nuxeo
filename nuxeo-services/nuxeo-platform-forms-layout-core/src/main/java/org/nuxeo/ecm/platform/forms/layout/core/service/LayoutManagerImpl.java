/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutManager;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class LayoutManagerImpl implements LayoutManager {

    private static final Log log = LogFactory.getLog(LayoutManagerImpl.class);

    private static final long serialVersionUID = 1L;

    protected final Map<String, WidgetType> widgetTypeRegistry;

    protected final Map<String, WidgetTypeDefinition> widgetTypeDefinitionRegistry;

    protected final Map<String, LayoutDefinition> layoutRegistry;

    protected final Map<String, WidgetDefinition> widgetRegistry;

    public LayoutManagerImpl() {
        widgetTypeDefinitionRegistry = new HashMap<String, WidgetTypeDefinition>();
        widgetTypeRegistry = new HashMap<String, WidgetType>();
        layoutRegistry = new HashMap<String, LayoutDefinition>();
        widgetRegistry = new HashMap<String, WidgetDefinition>();
    }

    // widget types

    public void registerWidgetType(WidgetTypeDefinition desc) {
        String name = desc.getName();
        String className = desc.getHandlerClassName();
        if (className == null) {
            log.error("Handler class missing " + "for widget type " + name);
            return;
        }
        Class<?> widgetTypeClass;
        try {
            // Thread context loader is not working in isolated EARs
            widgetTypeClass = LayoutManagerImpl.class.getClassLoader().loadClass(
                    className);
        } catch (Exception e) {
            log.error("Caught error when instantiating widget type handler", e);
            return;
        }

        // override only if handler class was resolved correctly
        if (widgetTypeRegistry.containsKey(name)
                || widgetTypeDefinitionRegistry.containsKey(name)) {
            log.warn(String.format("Overriding definition for widget type %s",
                    name));
            widgetTypeRegistry.remove(name);
            widgetTypeDefinitionRegistry.remove(name);
        }
        WidgetType widgetType = new WidgetTypeImpl(name, widgetTypeClass,
                desc.getProperties());
        widgetTypeRegistry.put(name, widgetType);
        widgetTypeDefinitionRegistry.put(name, desc);
        log.info("Registered widget type: " + name);
    }

    public void unregisterWidgetType(WidgetTypeDefinition desc) {
        String name = desc.getName();
        if (widgetTypeRegistry.containsKey(name)) {
            widgetTypeRegistry.remove(name);
            log.debug("Unregistered widget type: " + name);
        }
    }

    // layouts

    public void registerLayout(LayoutDefinition layoutDef) {
        String name = layoutDef.getName();
        if (layoutRegistry.containsKey(name)) {
            // TODO: implement merge
            layoutRegistry.remove(name);
        }
        layoutRegistry.put(name, layoutDef);
        log.info("Registered layout: " + name);
    }

    public void unregisterLayout(LayoutDefinition layoutDef) {
        String name = layoutDef.getName();
        if (layoutRegistry.containsKey(name)) {
            layoutRegistry.remove(name);
            log.debug("Unregistered layout: " + name);
        }
    }

    // widgets

    public void registerWidget(WidgetDefinition widgetDef) {
        String name = widgetDef.getName();
        if (widgetRegistry.containsKey(name)) {
            // TODO: implement merge
            widgetRegistry.remove(name);
        }
        widgetRegistry.put(name, widgetDef);
        log.info("Registered widget: " + name);
    }

    public void unregisterWidget(WidgetDefinition widgetDef) {
        String name = widgetDef.getName();
        if (widgetRegistry.containsKey(name)) {
            widgetRegistry.remove(name);
            log.debug("Unregistered widget: " + name);
        }
    }

    // service api

    public WidgetType getWidgetType(String typeName) {
        return widgetTypeRegistry.get(typeName);
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String typeName) {
        return widgetTypeDefinitionRegistry.get(typeName);
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions() {
        List<WidgetTypeDefinition> res = new ArrayList<WidgetTypeDefinition>();
        Collection<WidgetTypeDefinition> defs = widgetTypeDefinitionRegistry.values();
        if (defs != null) {
            res.addAll(defs);
        }
        return res;
    }

    public LayoutDefinition getLayoutDefinition(String layoutName) {
        return layoutRegistry.get(layoutName);
    }

    public List<String> getLayoutDefinitionNames() {
        return new ArrayList<String>(layoutRegistry.keySet());
    }

    public WidgetDefinition getWidgetDefinition(String widgetName) {
        return widgetRegistry.get(widgetName);
    }

}
