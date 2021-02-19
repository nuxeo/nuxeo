/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.styling.service.registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xerces.dom.DocumentImpl;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedMember;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.ecm.web.resources.core.ResourceBundleDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.service.descriptors.PageDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Registry for theme page resources.
 * <p>
 * Merges pages declaration with global page with name "*".
 * <p>
 * Forwards to {@link WebResourceManager} dedicated bundle registry for automated resource bundle declaration.
 *
 * @since 5.5
 */
public class PageRegistry extends MapRegistry<PageDescriptor> {

    protected static final String GLOBAL_CONFIG_NAME = "*";

    protected PageDescriptor globalPage;

    protected Map<String, PageDescriptor> mergedPages;

    protected static XAnnotatedObject<ResourceBundleDescriptor> xBundle;

    static {
        XMap fxmap = new XMap();
        fxmap.register(ResourceBundleDescriptor.class);
        xBundle = fxmap.getObject(ResourceBundleDescriptor.class);
    }

    @Override
    public void initialize() {
        super.initialize();
        mergedPages = Collections.synchronizedMap(new LinkedHashMap<>());
        globalPage = null;
        if (!disabled.contains(GLOBAL_CONFIG_NAME)) {
            globalPage = contributions.get(GLOBAL_CONFIG_NAME);
        }

        if (globalPage == null) {
            mergedPages.putAll(super.getContributions());
        } else {
            super.getContributions().forEach((k, v) -> {
                if (GLOBAL_CONFIG_NAME.equals(k)) {
                    mergedPages.put(k, v);
                } else {
                    // merge with global resources
                    mergedPages.put(k, mergePage(v, globalPage));
                }
            });
        }
    }

    @Override
    public Map<String, PageDescriptor> getContributions() {
        checkInitialized();
        return Collections.unmodifiableMap(mergedPages);
    }

    @Override
    public List<PageDescriptor> getContributionValues() {
        checkInitialized();
        return new ArrayList<>(mergedPages.values());
    }

    @Override
    public Optional<PageDescriptor> getContribution(String id) {
        checkInitialized();
        if (GLOBAL_CONFIG_NAME.contentEquals(id)) {
            return Optional.ofNullable(globalPage);
        }
        if (disabled.contains(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mergedPages.get(id));
    }

    public Registry<ResourceBundleDescriptor> getTargetRegistry() {
        return (Registry<ResourceBundleDescriptor>) Framework.getRuntime()
                                                             .getComponentManager()
                                                             .getExtensionPointRegistry(
                                                                     "org.nuxeo.ecm.platform.WebResources", "bundles")
                                                             .orElseThrow(() -> new IllegalArgumentException(
                                                                     "Unknown target registry"));
    }

    @Override
    protected PageDescriptor doRegister(Context ctx, XAnnotatedObject<PageDescriptor> xObject, Element element, String extensionId) {
        PageDescriptor page = super.doRegister(ctx, xObject, element, extensionId);
        if (page != null) {
            // forward bundle to WebResourceManager bundle registry, build DOM element from scratch
            Document xmlDoc = new DocumentImpl();
            Element root = xmlDoc.createElement("bundle");
            root.setAttribute("name", page.getComputedResourceBundleName());
            String doMerge = "false";
            XAnnotatedMember<Boolean> merge = xObject.getMerge();
            if (merge != null && Boolean.TRUE.equals(merge.getValue(ctx, element))) {
                doMerge = "true";
            }
            root.setAttribute("merge", doMerge);
            if (page.hasResources()) {
                Element resources = xmlDoc.createElement("resources");
                root.appendChild(resources);
                page.getResources().forEach(r -> {
                    Element resource = xmlDoc.createElement("resource");
                    resource.appendChild(xmlDoc.createTextNode(r));
                    resources.appendChild(resource);
                });
            }
            getTargetRegistry().register(ctx, xBundle, root, extensionId);
        }
        return page;
    }

    protected PageDescriptor mergePage(PageDescriptor page, PageDescriptor globalPage) {
        if (globalPage == null) {
            return page;
        }
        String charset = page.getCharset() != null ? page.getCharset() : globalPage.getCharset();
        String defaultFlavor = page.getDefaultFlavor() != null ? page.getDefaultFlavor()
                : globalPage.getDefaultFlavor();
        List<String> flavors = Stream.of(page.getFlavors(), globalPage.getFlavors())
                                     .flatMap(List::stream)
                                     .collect(Collectors.toList());
        List<String> resources = Stream.of(page.getResources(), globalPage.getResources())
                                       .flatMap(List::stream)
                                       .collect(Collectors.toList());
        List<String> bundles = Stream.of(page.getDeclaredResourceBundles(), globalPage.getDeclaredResourceBundles())
                                     .flatMap(List::stream)
                                     .collect(Collectors.toList());
        return new PageDescriptor(page.getName(), charset, defaultFlavor, flavors, resources, bundles);
    }

    public PageDescriptor getPage(String id) {
        return getContribution(id).orElse(null);
    }

    public List<PageDescriptor> getPages() {
        return getContributionValues();
    }

    /**
     * Returns all the page names.
     *
     * @since 7.10
     */
    public List<String> getPageNames() {
        return new ArrayList<>(getContributions().keySet());
    }

    public PageDescriptor getConfigurationApplyingToAll() {
        return getPage(GLOBAL_CONFIG_NAME);
    }

}
