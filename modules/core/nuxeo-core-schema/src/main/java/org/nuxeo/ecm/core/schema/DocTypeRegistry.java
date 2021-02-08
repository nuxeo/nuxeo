/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.schema;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Registry for multiple descriptors to the "doctype" extension point.
 * <p>
 * Handles custom merges.
 *
 * @since 11.5
 */
public class DocTypeRegistry implements Registry {

    protected DocumentTypeRegistry typeRegistry = new DocumentTypeRegistry();

    protected MapRegistry facetRegistry = new MapRegistry();

    protected MapRegistry proxyRegistry = new MapRegistry();

    @Override
    public void initialize() {
        typeRegistry.initialize();
        facetRegistry.initialize();
        proxyRegistry.initialize();
    }

    @Override
    public void tag(String id) {
        typeRegistry.tag(id);
        facetRegistry.tag(id);
        proxyRegistry.tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return typeRegistry.isTagged(id) || facetRegistry.isTagged(id) || proxyRegistry.isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        Class<?> klass = xObject.getKlass();
        if (DocumentTypeDescriptor.class.equals(klass)) {
            typeRegistry.register(ctx, xObject, element, tag);
        } else if (FacetDescriptor.class.equals(klass)) {
            facetRegistry.register(ctx, xObject, element, tag);
        } else if (ProxiesDescriptor.class.equals(klass)) {
            proxyRegistry.register(ctx, xObject, element, tag);
        } else {
            throw new IllegalArgumentException("Unsupported class " + klass);
        }
    }

    @Override
    public void unregister(String tag) {
        typeRegistry.unregister(tag);
        facetRegistry.unregister(tag);
        proxyRegistry.unregister(tag);
    }

    // custom API

    /**
     * Register method used to synchronize allowed/denied subtypes definition from higher-level TypeService.
     */
    public void registerDocumentType(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        typeRegistry.registerDocumentType(ctx, xObject, element, tag);
    }

    /**
     * Unregister method used to synchronize allowed/denied subtypes definition from higher-level TypeService.
     */
    public void unregisterDocumentType(String tag) {
        typeRegistry.unregisterDocumentType(tag);
    }

    public Map<String, DocumentTypeDescriptor> getDocumentTypes() {
        return typeRegistry.getContributions();
    }

    public List<ProxiesDescriptor> getProxies() {
        return proxyRegistry.getContributionValues();
    }

    public List<FacetDescriptor> getFacets() {
        return facetRegistry.getContributionValues();
    }

    public Set<String> getDisabledFacets() {
        return facetRegistry.getDisabledContributions();
    }

}
