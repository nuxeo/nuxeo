/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeImpl;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutConverterRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutTypeDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetConverterRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetTypeDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetTypeRegistry;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutConverterDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetConverterDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
public class LayoutStoreImpl extends DefaultComponent implements LayoutStore {

    private static final Logger log = LogManager.getLogger(LayoutStoreImpl.class);

    private static final long serialVersionUID = 1L;

    public static final String WIDGET_TYPES_EP_NAME = "widgettypes";

    /**
     * @since 6.0
     */
    public static final String LAYOUT_TYPES_EP_NAME = "layouttypes";

    public static final String WIDGETS_EP_NAME = "widgets";

    public static final String LAYOUTS_EP_NAME = "layouts";

    public static final String LAYOUT_CONVERTERS_EP_NAME = "layoutConverters";

    public static final String WIDGET_CONVERTERS_EP_NAME = "widgetConverters";

    protected final Map<String, WidgetTypeRegistry> widgetTypesByCat;

    protected final Map<String, WidgetTypeDefinitionRegistry> widgetTypeDefsByCat;

    protected final Map<String, LayoutTypeDefinitionRegistry> layoutTypeDefsByCat;

    protected final Map<String, LayoutDefinitionRegistry> layoutsByCat;

    protected final Map<String, WidgetDefinitionRegistry> widgetsByCat;

    protected final Map<String, WidgetConverterRegistry> widgetConvertersByCat;

    protected final Map<String, LayoutConverterRegistry> layoutConvertersByCat;

    public LayoutStoreImpl() {
        widgetTypeDefsByCat = new HashMap<>();
        layoutTypeDefsByCat = new HashMap<>();
        widgetTypesByCat = new HashMap<>();
        layoutsByCat = new HashMap<>();
        widgetsByCat = new HashMap<>();
        widgetConvertersByCat = new HashMap<>();
        layoutConvertersByCat = new HashMap<>();
    }

    // Runtime component API

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            WidgetTypeDescriptor desc = (WidgetTypeDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register widget type: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerWidgetType(cat, desc.getWidgetTypeDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUT_TYPES_EP_NAME)) {
            LayoutTypeDescriptor desc = (LayoutTypeDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register layout type: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerLayoutType(cat, desc.getLayoutTypeDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            LayoutDescriptor desc = (LayoutDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register layout: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerLayout(cat, desc.getLayoutDefinition());
                }
            }
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            WidgetDescriptor desc = (WidgetDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register widget: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerWidget(cat, desc.getWidgetDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUT_CONVERTERS_EP_NAME)) {
            LayoutConverterDescriptor desc = (LayoutConverterDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register layout converter: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerLayoutConverter(cat, desc);
                }
            }
        } else if (extensionPoint.equals(WIDGET_CONVERTERS_EP_NAME)) {
            WidgetConverterDescriptor desc = (WidgetConverterDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register widget converter: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    registerWidgetConverter(cat, desc);
                }
            }
        } else {
            log.error("Unknown extension point: {}, can't register !", extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            WidgetTypeDescriptor desc = (WidgetTypeDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot unregister widget type: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterWidgetType(cat, desc.getWidgetTypeDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUT_TYPES_EP_NAME)) {
            LayoutTypeDescriptor desc = (LayoutTypeDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot unregister layout type: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterLayoutType(cat, desc.getLayoutTypeDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            LayoutDescriptor desc = (LayoutDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot unregister layout: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterLayout(cat, desc.getLayoutDefinition());
                }
            }
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            WidgetDescriptor desc = (WidgetDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot unregister widget: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterWidget(cat, desc.getWidgetDefinition());
                }
            }
        } else if (extensionPoint.equals(LAYOUT_CONVERTERS_EP_NAME)) {
            LayoutConverterDescriptor desc = (LayoutConverterDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register layout converter: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterLayoutConverter(cat, desc);
                }
            }
        } else if (extensionPoint.equals(WIDGET_CONVERTERS_EP_NAME)) {
            WidgetConverterDescriptor desc = (WidgetConverterDescriptor) contribution;
            String[] categories = desc.getCategories();
            if (categories == null || categories.length == 0) {
                log.error("Cannot register widget converter: {}, no category found", desc::getName);
            } else {
                for (String cat : categories) {
                    unregisterWidgetConverter(cat, desc);
                }
            }
        } else {
            log.error("Unknown extension point: {}, can't unregister !", extensionPoint);
        }
    }

    // Categories

    @Override
    public List<String> getCategories() {
        Set<String> cats = new HashSet<>();
        cats.addAll(widgetTypeDefsByCat.keySet());
        cats.addAll(widgetTypesByCat.keySet());
        cats.addAll(layoutsByCat.keySet());
        cats.addAll(widgetsByCat.keySet());
        List<String> res = new ArrayList<>();
        res.addAll(cats);
        Collections.sort(res);
        return res;
    }

    // widget types

    @Override
    public void registerWidgetType(String category, WidgetTypeDefinition desc) {
        String name = desc.getName();
        String className = desc.getHandlerClassName();
        Class<?> widgetTypeClass = null;
        if (className != null) {
            try {
                widgetTypeClass = LayoutStoreImpl.class.getClassLoader().loadClass(className);
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instantiating widget type handler", e);
                return;
            }
        }

        // override only if handler class was resolved correctly
        if (widgetTypesByCat.containsKey(name) || widgetTypeDefsByCat.containsKey(name)) {
            log.warn("Overriding definition for widget type: {}", name);
            widgetTypesByCat.remove(name);
            widgetTypeDefsByCat.remove(name);
        }
        WidgetTypeImpl widgetType = new WidgetTypeImpl(name, widgetTypeClass, desc.getProperties());
        widgetType.setAliases(desc.getAliases());
        WidgetTypeRegistry typeReg = widgetTypesByCat.get(category);
        if (typeReg == null) {
            typeReg = new WidgetTypeRegistry(category);
            widgetTypesByCat.put(category, typeReg);
        }
        typeReg.addContribution(widgetType);
        WidgetTypeDefinitionRegistry defReg = widgetTypeDefsByCat.get(category);
        if (defReg == null) {
            defReg = new WidgetTypeDefinitionRegistry(category);
            widgetTypeDefsByCat.put(category, defReg);
        }
        defReg.addContribution(desc);
        log.info("Registered widget type: {} for category: {}", name, category);
    }

    @Override
    public void unregisterWidgetType(String category, WidgetTypeDefinition desc) {
        String name = desc.getName();
        WidgetTypeRegistry typeReg = widgetTypesByCat.get(category);
        WidgetTypeDefinitionRegistry defReg = widgetTypeDefsByCat.get(category);
        if (typeReg != null && defReg != null) {
            // remove corresponding widget type, only reuse name
            WidgetType widgetType = new WidgetTypeImpl(name, null, null);
            typeReg.removeContribution(widgetType);
            defReg.removeContribution(desc);
            log.info("Unregistered widget type: {} for category: {}", name, category);
        }
    }

    // layout types

    @Override
    public void registerLayoutType(String category, LayoutTypeDefinition layoutTypeDef) {
        LayoutTypeDefinitionRegistry reg = layoutTypeDefsByCat.get(category);
        if (reg == null) {
            reg = new LayoutTypeDefinitionRegistry(category);
            layoutTypeDefsByCat.put(category, reg);
        }
        reg.addContribution(layoutTypeDef);
        log.info("Registered layout type: {} for category: {}", layoutTypeDef.getName(), category);
    }

    @Override
    public void unregisterLayoutType(String category, LayoutTypeDefinition layoutTypeDef) {
        LayoutTypeDefinitionRegistry reg = layoutTypeDefsByCat.get(category);
        if (reg != null) {
            reg.removeContribution(layoutTypeDef);
            log.info("Unregistered layout type: {} for category: {}", layoutTypeDef.getName(), category);
        }
    }

    // layouts

    @Override
    public void registerLayout(String category, LayoutDefinition layoutDef) {
        LayoutDefinitionRegistry reg = layoutsByCat.get(category);
        if (reg == null) {
            reg = new LayoutDefinitionRegistry(category);
            layoutsByCat.put(category, reg);
        }
        reg.addContribution(layoutDef);
        log.info("Registered layout: {} for category: {}", layoutDef.getName(), category);
    }

    @Override
    public void unregisterLayout(String category, LayoutDefinition layoutDef) {
        LayoutDefinitionRegistry reg = layoutsByCat.get(category);
        if (reg != null) {
            reg.removeContribution(layoutDef);
            log.info("Unregistered layout: {} for category: {}", layoutDef.getName(), category);
        }
    }

    // widgets

    @Override
    public void registerWidget(String category, WidgetDefinition widgetDef) {
        WidgetDefinitionRegistry reg = widgetsByCat.get(category);
        if (reg == null) {
            reg = new WidgetDefinitionRegistry(category);
            widgetsByCat.put(category, reg);
        }
        reg.addContribution(widgetDef);
        log.info("Registered widget: {} for category: {}", widgetDef.getName(), category);
    }

    @Override
    public void unregisterWidget(String category, WidgetDefinition widgetDef) {
        WidgetDefinitionRegistry reg = widgetsByCat.get(category);
        if (reg != null) {
            reg.removeContribution(widgetDef);
            log.info("Unregistered widget: {} for category: {}", widgetDef.getName(), category);
        }
    }

    // converter descriptors

    public void registerLayoutConverter(String category, LayoutConverterDescriptor layoutConverter) {
        LayoutConverterRegistry reg = layoutConvertersByCat.get(category);
        if (reg == null) {
            reg = new LayoutConverterRegistry(category);
            layoutConvertersByCat.put(category, reg);
        }
        reg.addContribution(layoutConverter);
        log.info("Registered layout converter: {} for category: {}", layoutConverter.getName(), category);
    }

    public void unregisterLayoutConverter(String category, LayoutConverterDescriptor layoutConverter) {
        LayoutConverterRegistry reg = layoutConvertersByCat.get(category);
        if (reg != null) {
            reg.removeContribution(layoutConverter);
            log.info("Unregistered layout converter: {} for category: {}", layoutConverter.getName(), category);
        }
    }

    public void registerWidgetConverter(String category, WidgetConverterDescriptor widgetConverter) {
        WidgetConverterRegistry reg = widgetConvertersByCat.get(category);
        if (reg == null) {
            reg = new WidgetConverterRegistry(category);
            widgetConvertersByCat.put(category, reg);
        }
        reg.addContribution(widgetConverter);
        log.info("Registered widget converter: {} for category: {}", widgetConverter.getName(), category);
    }

    public void unregisterWidgetConverter(String category, WidgetConverterDescriptor widgetConverter) {
        WidgetConverterRegistry reg = widgetConvertersByCat.get(category);
        if (reg != null) {
            reg.removeContribution(widgetConverter);
            log.info("Unregistered widget converter: {} for category: {}", widgetConverter.getName(), category);
        }
    }

    // service api

    @Override
    public WidgetType getWidgetType(String category, String typeName) {
        WidgetTypeRegistry reg = widgetTypesByCat.get(category);
        if (reg != null) {
            return reg.getWidgetType(typeName);
        }
        return null;
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String category, String typeName) {
        WidgetTypeDefinitionRegistry reg = widgetTypeDefsByCat.get(category);
        if (reg != null) {
            return reg.getDefinition(typeName);
        }
        return null;
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions(String category) {
        List<WidgetTypeDefinition> res = new ArrayList<>();
        WidgetTypeDefinitionRegistry reg = widgetTypeDefsByCat.get(category);
        if (reg != null) {
            Collection<WidgetTypeDefinition> defs = reg.getDefinitions();
            if (defs != null) {
                res.addAll(defs);
            }
        }
        return res;
    }

    @Override
    public LayoutTypeDefinition getLayoutTypeDefinition(String category, String typeName) {
        LayoutTypeDefinitionRegistry reg = layoutTypeDefsByCat.get(category);
        if (reg != null) {
            return reg.getDefinition(typeName);
        }
        return null;
    }

    @Override
    public List<LayoutTypeDefinition> getLayoutTypeDefinitions(String category) {
        List<LayoutTypeDefinition> res = new ArrayList<>();
        LayoutTypeDefinitionRegistry reg = layoutTypeDefsByCat.get(category);
        if (reg != null) {
            Collection<LayoutTypeDefinition> defs = reg.getDefinitions();
            if (defs != null) {
                res.addAll(defs);
            }
        }
        return res;
    }

    @Override
    public LayoutDefinition getLayoutDefinition(String category, String layoutName) {
        LayoutDefinitionRegistry reg = layoutsByCat.get(category);
        if (reg != null) {
            return reg.getLayoutDefinition(layoutName);
        }
        return null;
    }

    @Override
    public List<String> getLayoutDefinitionNames(String category) {
        LayoutDefinitionRegistry reg = layoutsByCat.get(category);
        if (reg != null) {
            return reg.getLayoutNames();
        }
        return Collections.emptyList();
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String category, String widgetName) {
        WidgetDefinitionRegistry reg = widgetsByCat.get(category);
        if (reg != null) {
            return reg.getWidgetDefinition(widgetName);
        }
        return null;
    }

    @Override
    public List<LayoutDefinitionConverter> getLayoutConverters(String category) {
        List<LayoutDefinitionConverter> res = new ArrayList<>();
        List<String> orderedConverterNames = new ArrayList<>();
        LayoutConverterRegistry reg = layoutConvertersByCat.get(category);
        if (reg != null) {
            List<LayoutConverterDescriptor> descs = reg.getConverters();
            // first sort by order
            Collections.sort(descs);
            // instantiate converter instances
            for (LayoutConverterDescriptor desc : descs) {
                Class<?> converterClass;
                try {
                    converterClass = LayoutStoreImpl.class.getClassLoader().loadClass(desc.getConverterClassName());
                    LayoutDefinitionConverter converter = (LayoutDefinitionConverter) converterClass.getDeclaredConstructor()
                                                                                                    .newInstance();
                    res.add(converter);
                    orderedConverterNames.add(desc.getName());
                } catch (ReflectiveOperationException e) {
                    log.error("Caught error when instantiating layout definition converter", e);
                }
            }
        }
        log.debug("Ordered layout converters for category {}: {}", category, orderedConverterNames);
        return res;
    }

    @Override
    public List<WidgetDefinitionConverter> getWidgetConverters(String category) {
        List<WidgetDefinitionConverter> res = new ArrayList<>();
        List<String> orderedConverterNames = new ArrayList<>();
        WidgetConverterRegistry reg = widgetConvertersByCat.get(category);
        if (reg != null) {
            List<WidgetConverterDescriptor> descs = reg.getConverters();
            // first sort by order
            Collections.sort(descs);
            // instantiate converter instances
            for (WidgetConverterDescriptor desc : descs) {
                Class<?> converterClass;
                try {
                    converterClass = LayoutStoreImpl.class.getClassLoader().loadClass(desc.getConverterClassName());
                    WidgetDefinitionConverter converter = (WidgetDefinitionConverter) converterClass.getDeclaredConstructor()
                                                                                                    .newInstance();
                    res.add(converter);
                    orderedConverterNames.add(desc.getName());
                } catch (ReflectiveOperationException e) {
                    log.error("Caught error when instantiating widget definition converter", e);
                }
            }
        }
        log.debug("Ordered widget converters for category {}: {}", category, orderedConverterNames);
        return res;
    }

}
