/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ProcessorDescriptor;
import org.nuxeo.ecm.web.resources.core.ResourceBundleDescriptor;
import org.nuxeo.ecm.web.resources.core.ResourceDescriptor;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.3
 */
public class WebResourceManagerImpl extends DefaultComponent implements WebResourceManager {

    private static final Logger log = LogManager.getLogger(WebResourceManagerImpl.class);

    /** @since 11.5 */
    public static final String COMPONENT_NAME = "org.nuxeo.ecm.platform.WebResources";

    /** @since 11.5 */
    public static final String RESOURCES_ENDPOINT = "resources";

    protected static final String RESOURCE_BUNDLES_ENDPOINT = "bundles";

    protected static final String PROCESSORS_ENDPOINT = "processors";

    // service API

    @Override
    public Resource getResource(String name) {
        return this.<ResourceDescriptor> getRegistryContribution(RESOURCES_ENDPOINT, name).orElse(null);
    }

    @Override
    public ResourceBundle getResourceBundle(String name) {
        return this.<ResourceBundleDescriptor> getRegistryContribution(RESOURCE_BUNDLES_ENDPOINT, name).orElse(null);
    }

    @Override
    public List<ResourceBundle> getResourceBundles() {
        return getRegistryContributions(RESOURCE_BUNDLES_ENDPOINT);
    }

    @Override
    public Processor getProcessor(String name) {
        return this.<ProcessorDescriptor> getRegistryContribution(PROCESSORS_ENDPOINT, name).orElse(null);
    }

    @Override
    public List<Processor> getProcessors() {
        return getRegistryContributions(PROCESSORS_ENDPOINT);
    }

    @Override
    public List<Processor> getProcessors(String type) {
        return getProcessors().stream()
                              .filter(p -> type == null || p.getTypes().contains(type))
                              .sorted()
                              .collect(Collectors.toList());
    }

    @Override
    public List<Resource> getResources(ResourceContext context, String bundleName, String type) {
        List<Resource> res = new ArrayList<>();
        ResourceBundle rb = getResourceBundle(bundleName);
        if (rb == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Unknown bundle named '%s'", bundleName));
            }
            return res;
        }

        Map<String, Resource> all = new HashMap<>();
        // retrieve deps + filter depending on type + detect cycles
        DirectedAcyclicGraph<String, DefaultEdge> graph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        for (String rn : rb.getResources()) {
            Resource r = getResource(rn);
            if (r == null) {
                log.error("Could not resolve resource '{}' on bundle '{}'", rn, bundleName);
                continue;
            }
            // resolve sub resources of given type before filtering
            Map<String, Resource> subRes = getSubResources(graph, r, type);
            if (ResourceType.matches(type, r) || !subRes.isEmpty()) {
                graph.addVertex(rn);
                all.put(rn, r);
                all.putAll(subRes);
            }
        }

        for (String rn : graph) { // iterates in topological order
            Resource r = all.get(rn);
            if (ResourceType.matches(type, r)) {
                res.add(r);
            }
        }

        return res;
    }

    protected Map<String, Resource> getSubResources(DirectedAcyclicGraph<String, DefaultEdge> graph, Resource r,
            String type) {
        Map<String, Resource> res = new HashMap<>();
        List<String> deps = r.getDependencies();
        if (deps != null) {
            for (String dn : deps) {
                Resource d = getResource(dn);
                if (d == null) {
                    log.error("Unknown resource dependency named '{}'", dn);
                    continue;
                }
                if (!ResourceType.matches(type, d)) {
                    continue;
                }
                res.put(dn, d);
                graph.addVertex(dn);
                graph.addVertex(r.getName());
                try {
                    graph.addEdge(dn, r.getName());
                } catch (IllegalArgumentException e) {
                    log.error("Cycle detected in resource dependencies: ", e);
                    break;
                }
                res.putAll(getSubResources(graph, d, type));
            }
        }
        return res;
    }

}
