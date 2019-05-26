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
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.tree.DefaultDocumentTreeFilter;
import org.nuxeo.ecm.core.api.tree.DefaultDocumentTreeSorter;
import org.nuxeo.ecm.core.api.tree.DocumentTreeFilter;
import org.nuxeo.ecm.core.api.tree.DocumentTreeSorter;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component for tree service, handing registries for trees managing {@link DocumentModel} object.
 *
 * @author Anahide Tchertchian
 */
// TODO: refactor to use only one registry
public class TreeManagerImpl extends DefaultComponent implements TreeManager {

    private static final long serialVersionUID = 1L;

    public static final String NAME = TreeManager.class.getName();

    public static final String PLUGIN_EXTENSION_POINT = "plugin";

    private static final Log log = LogFactory.getLog(TreeManager.class);

    protected Map<String, Filter> filters;

    protected Map<String, Filter> leafFilters;

    protected Map<String, Sorter> sorters;

    protected Map<String, String> pageProviderNames;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(TreeManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        filters = new HashMap<>();
        leafFilters = new HashMap<>();
        sorters = new HashMap<>();
        pageProviderNames = new HashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        filters = null;
        leafFilters = null;
        sorters = null;
        pageProviderNames = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PLUGIN_EXTENSION_POINT.equals(extensionPoint)) {
            TreeManagerPluginDescriptor plugin = (TreeManagerPluginDescriptor) contribution;
            String name = plugin.getName();
            // filter
            if (filters.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding filter for plugin " + name);
                filters.remove(name);
            }
            log.info("Registering filter for plugin " + name);
            filters.put(name, buildFilter(plugin));
            // leaf filter
            if (leafFilters.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding leaf filter for plugin " + name);
                leafFilters.remove(name);
            }
            log.info("Registering leaf filter for plugin " + name);
            leafFilters.put(name, buildLeafFilter(plugin));
            // sorter
            if (sorters.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding sorter for plugin " + name);
                sorters.remove(name);
            }
            log.info("Registering sorter for plugin " + name);
            sorters.put(name, buildSorter(plugin));

            // page provider
            if (pageProviderNames.containsKey(name)) {
                // FIXME handle merge?
                log.info("Overriding page provider for plugin " + name);
                pageProviderNames.remove(name);
            }
            log.info("Registering page provider for plugin " + name);
            pageProviderNames.put(name, plugin.getPageProvider());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PLUGIN_EXTENSION_POINT.equals(extensionPoint)) {
            TreeManagerPluginDescriptor plugin = (TreeManagerPluginDescriptor) contribution;
            String name = plugin.getName();
            // filter
            if (filters.containsKey(name)) {
                log.info("Unregistering filter for plugin " + name);
                filters.remove(name);
            }
            // leaf filter
            if (leafFilters.containsKey(name)) {
                log.info("Unregistering leaf filter for plugin " + name);
                leafFilters.remove(name);
            }
            // sorter
            if (sorters.containsKey(name)) {
                log.info("Unregistering sorter for plugin " + name);
                sorters.remove(name);
            }
            // page provider
            if (pageProviderNames.containsKey(name)) {
                log.info("Unregistering page provider for plugin " + name);
                pageProviderNames.remove(name);
            }
        }
    }

    protected Filter buildFilter(TreeManagerPluginDescriptor plugin) {
        Filter filter = null;

        List<String> includedFacets = plugin.getIncludedFacets();
        List<String> excludedFacets = plugin.getExcludedFacets();
        List<String> excludedTypes = plugin.getExcludedTypes();

        String filterClass = plugin.getFilterClassName();
        if (filterClass == null || "".equals(filterClass)) {
            if ((includedFacets == null || includedFacets.isEmpty())
                    && (excludedFacets == null || excludedFacets.isEmpty())
                    && (excludedTypes == null || excludedTypes.isEmpty())) {
                return null;
            }
            // built-in filter
            filter = new DefaultDocumentTreeFilter();
        } else {
            // custom filter
            try {
                Object instance = TreeManagerImpl.class.getClassLoader()
                                                       .loadClass(filterClass)
                                                       .getDeclaredConstructor()
                                                       .newInstance();
                if (instance instanceof Filter) {
                    filter = (Filter) instance;
                } else {
                    log.error(String.format("Class %s should follow %s interface", filterClass, Filter.class.getName()));
                }
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }

        // setup config when possible
        if (filter instanceof DocumentTreeFilter) {
            DocumentTreeFilter treeFilter = (DocumentTreeFilter) filter;
            treeFilter.setIncludedFacets(includedFacets);
            treeFilter.setExcludedFacets(excludedFacets);
            treeFilter.setExcludedTypes(excludedTypes);
        }

        return filter;
    }

    protected Filter buildLeafFilter(TreeManagerPluginDescriptor plugin) {
        String leafFilterClass = plugin.getLeafFilterClassName();
        if (leafFilterClass == null || "".equals(leafFilterClass)) {
            return null;
        }
        try {
            Object instance = TreeManagerImpl.class.getClassLoader()
                                                   .loadClass(leafFilterClass)
                                                   .getDeclaredConstructor()
                                                   .newInstance();
            if (instance instanceof Filter) {
                return (Filter) instance;
            } else {
                log.error(String.format("Class %s should follow %s interface", leafFilterClass, Filter.class.getName()));
            }
        } catch (ReflectiveOperationException e) {
            log.error(e, e);
        }
        return null;
    }

    protected Sorter buildSorter(TreeManagerPluginDescriptor plugin) {
        Sorter sorter = null;

        String sortPropertyPath = plugin.getSortPropertyPath();

        String sorterClass = plugin.getSorterClassName();
        if (sorterClass == null || "".equals(sorterClass)) {
            if (sortPropertyPath == null || "".equals(sortPropertyPath)) {
                return null;
            }
            // built-in sorter
            sorter = new DefaultDocumentTreeSorter();
        } else {
            // custom sorter
            try {
                Object instance = TreeManagerImpl.class.getClassLoader()
                                                       .loadClass(sorterClass)
                                                       .getDeclaredConstructor()
                                                       .newInstance();
                if (instance instanceof Sorter) {
                    sorter = (Sorter) instance;
                } else {
                    log.error(String.format("Class %s should follow %s interface", sorterClass, Sorter.class.getName()));
                }
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
            }
        }

        // setup config when possible
        if (sorter instanceof DocumentTreeSorter) {
            DocumentTreeSorter treeSorter = (DocumentTreeSorter) sorter;
            treeSorter.setSortPropertyPath(sortPropertyPath);
        }

        return sorter;
    }

    @Override
    public Filter getFilter(String pluginName) {
        return filters.get(pluginName);
    }

    @Override
    public Filter getLeafFilter(String pluginName) {
        return leafFilters.get(pluginName);
    }

    @Override
    public Sorter getSorter(String pluginName) {
        return sorters.get(pluginName);
    }

    @Override
    public String getPageProviderName(String pluginName) {
        return pageProviderNames.get(pluginName);
    }

}
