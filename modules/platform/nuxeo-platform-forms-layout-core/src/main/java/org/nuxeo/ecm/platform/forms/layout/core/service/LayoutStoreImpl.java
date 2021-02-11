/*
 * (C) Copyright 2010-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.nuxeo.ecm.platform.forms.layout.core.registries.AbstractCategoryMapRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutConverterRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.LayoutTypeDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetConverterRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.core.registries.WidgetTypeDefinitionRegistry;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutConverterDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetConverterDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Manages layout-related registries.
 *
 * @since 5.5
 */
public class LayoutStoreImpl extends DefaultComponent implements LayoutStore {

    private static final Logger log = LogManager.getLogger(LayoutStoreImpl.class);

    public static final String WIDGET_TYPES_EP_NAME = "widgettypes";

    /** @since 6.0 */
    public static final String LAYOUT_TYPES_EP_NAME = "layouttypes";

    public static final String WIDGETS_EP_NAME = "widgets";

    public static final String LAYOUTS_EP_NAME = "layouts";

    public static final String LAYOUT_CONVERTERS_EP_NAME = "layoutConverters";

    public static final String WIDGET_CONVERTERS_EP_NAME = "widgetConverters";

    protected Map<String, Map<String, WidgetType>> widgetTypesByCat;

    protected Map<String, List<WidgetDefinitionConverter>> widgetConvertersByCat;

    protected Map<String, List<LayoutDefinitionConverter>> layoutConvertersByCat;

    // Runtime component API

    @Override
    public void start(ComponentContext context) {
        initWidgetTypes();
        initWidgetConverters();
        initLayoutConverters();
    }

    protected void initWidgetTypes() {
        widgetTypesByCat = new HashMap<>();
        WidgetTypeDefinitionRegistry widgetTypeReg = getExtensionPointRegistry(WIDGET_TYPES_EP_NAME);
        widgetTypeReg.getCategories().forEach(cat -> {
            List<WidgetTypeDefinition> descs = widgetTypeReg.getContributionValues(cat);
            descs.forEach(desc -> {
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

                String name = desc.getName();
                WidgetTypeImpl widgetType = new WidgetTypeImpl(name, widgetTypeClass, desc.getProperties());
                List<String> aliases = desc.getAliases();
                widgetType.setAliases(aliases);
                widgetTypesByCat.computeIfAbsent(cat, k -> new HashMap<>()).put(name, widgetType);
                aliases.forEach(alias -> widgetTypesByCat.get(cat).put(alias, widgetType));
            });
        });
    }

    protected void initWidgetConverters() {
        widgetConvertersByCat = new HashMap<>();
        WidgetConverterRegistry reg = getExtensionPointRegistry(WIDGET_CONVERTERS_EP_NAME);
        reg.getCategories().forEach(cat -> {
            List<WidgetConverterDescriptor> descs = reg.getContributionValues(cat);
            List<WidgetDefinitionConverter> converters = new ArrayList<>();
            List<String> names = new ArrayList<>();
            descs.stream().sorted().forEach(desc -> {
                String name = desc.getName();
                try {
                    Class<?> converterClass = LayoutStoreImpl.class.getClassLoader()
                                                                   .loadClass(desc.getConverterClassName());
                    converters.add((WidgetDefinitionConverter) converterClass.getDeclaredConstructor().newInstance());
                    names.add(name);
                } catch (ReflectiveOperationException | ClassCastException e) {
                    log.error("Caught error when instantiating widget definition converter {}", name, e);
                    return;
                }
            });
            log.debug("Ordered widget converters for category '{}': {}", cat, names);
            widgetConvertersByCat.put(cat, converters);
        });
    }

    protected void initLayoutConverters() {
        layoutConvertersByCat = new HashMap<>();
        LayoutConverterRegistry reg = getExtensionPointRegistry(LAYOUT_CONVERTERS_EP_NAME);
        reg.getCategories().forEach(cat -> {
            List<LayoutConverterDescriptor> descs = reg.getContributionValues(cat);
            List<LayoutDefinitionConverter> converters = new ArrayList<>();
            List<String> names = new ArrayList<>();
            descs.stream().sorted().forEach(desc -> {
                String name = desc.getName();
                try {
                    Class<?> converterClass = LayoutStoreImpl.class.getClassLoader()
                                                                   .loadClass(desc.getConverterClassName());
                    converters.add((LayoutDefinitionConverter) converterClass.getDeclaredConstructor().newInstance());
                    names.add(name);
                } catch (ReflectiveOperationException | ClassCastException e) {
                    log.error("Caught error when instantiating layout definition converter {}", name, e);
                    return;
                }
            });
            log.debug("Ordered layout converters for category '{}': {}", cat, names);
            layoutConvertersByCat.put(cat, converters);
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        widgetTypesByCat = null;
        widgetConvertersByCat = null;
        layoutConvertersByCat = null;
    }

    // Categories

    @Override
    public List<String> getCategories() {
        return Stream.<AbstractCategoryMapRegistry> of(getExtensionPointRegistry(WIDGET_TYPES_EP_NAME),
                getExtensionPointRegistry(LAYOUTS_EP_NAME), getExtensionPointRegistry(WIDGETS_EP_NAME))
                     .map(AbstractCategoryMapRegistry::getCategories)
                     .flatMap(List::stream)
                     .distinct()
                     .sorted()
                     .collect(Collectors.toList());
    }

    // service api

    @Override
    public WidgetType getWidgetType(String category, String typeName) {
        return widgetTypesByCat.getOrDefault(category, Collections.emptyMap()).get(typeName);
    }

    @Override
    public WidgetTypeDefinition getWidgetTypeDefinition(String category, String typeName) {
        WidgetTypeDefinitionRegistry widgetTypeReg = getExtensionPointRegistry(WIDGET_TYPES_EP_NAME);
        return widgetTypeReg.getContribution(category, typeName);
    }

    @Override
    public List<WidgetTypeDefinition> getWidgetTypeDefinitions(String category) {
        WidgetTypeDefinitionRegistry widgetTypeReg = getExtensionPointRegistry(WIDGET_TYPES_EP_NAME);
        return widgetTypeReg.getContributionValues(category);
    }

    @Override
    public LayoutTypeDefinition getLayoutTypeDefinition(String category, String typeName) {
        LayoutTypeDefinitionRegistry reg = getExtensionPointRegistry(LAYOUT_TYPES_EP_NAME);
        return reg.getContribution(category, typeName);
    }

    @Override
    public List<LayoutTypeDefinition> getLayoutTypeDefinitions(String category) {
        LayoutTypeDefinitionRegistry reg = getExtensionPointRegistry(LAYOUT_TYPES_EP_NAME);
        return reg.getContributionValues(category);
    }

    @Override
    public LayoutDefinition getLayoutDefinition(String category, String layoutName) {
        LayoutDefinitionRegistry reg = getExtensionPointRegistry(LAYOUTS_EP_NAME);
        return reg.getContribution(category, layoutName);
    }

    @Override
    public List<String> getLayoutDefinitionNames(String category) {
        LayoutDefinitionRegistry reg = getExtensionPointRegistry(LAYOUTS_EP_NAME);
        return new ArrayList<>(reg.getContributions(category).keySet());
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String category, String widgetName) {
        WidgetDefinitionRegistry reg = getExtensionPointRegistry(WIDGETS_EP_NAME);
        return reg.getContribution(category, widgetName);
    }

    @Override
    public List<LayoutDefinitionConverter> getLayoutConverters(String category) {
        return Collections.unmodifiableList(layoutConvertersByCat.getOrDefault(category, Collections.emptyList()));
    }

    @Override
    public List<WidgetDefinitionConverter> getWidgetConverters(String category) {
        return Collections.unmodifiableList(widgetConvertersByCat.getOrDefault(category, Collections.emptyList()));
    }

}
