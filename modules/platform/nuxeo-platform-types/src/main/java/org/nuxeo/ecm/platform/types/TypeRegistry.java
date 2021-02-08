/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.xerces.dom.DocumentImpl;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.DOMHelper;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.schema.DocTypeRegistry;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TypeRegistry extends MapRegistry {

    protected static XAnnotatedObject xCoreType;

    static {
        XMap xmap = new XMap();
        xmap.register(DocumentTypeDescriptor.class);
        xCoreType = xmap.getObject(DocumentTypeDescriptor.class);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        super.register(ctx, xObject, element, tag);
        registerCoreContribution(ctx, element, tag);
    }

    @Override
    public void unregister(String tag) {
        super.unregister(tag);
        getCoreRegistry().unregister(tag);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        Type merged = super.getMergedInstance(ctx, xObject, element, existing);
        // delete denied subtypes from allowed subtypes
        Set<String> denied = Set.of(merged.getDeniedSubTypes());
        merged.getAllowedSubTypes().keySet().removeIf(Predicate.not(denied::contains));
        return (T) merged;
    }

    // custom API

    public boolean hasType(String id) {
        return getContribution(id).isPresent();
    }

    public Collection<Type> getTypes() {
        return getContributionValues();
    }

    public Type getType(String id) {
        return this.<Type> getContribution(id).orElse(null);
    }

    /**
     * @since 8.10
     */
    protected void recomputeTypes() {
        List<Type> types = getContributionValues();
        for (Type type : types) {
            type.setAllowedSubTypes(getCoreAllowedSubtypes(type));
            // do not need to add denied subtypes because allowed subtypes already come filtered from core
            type.setDeniedSubTypes(new String[0]);
        }
    }

    /**
     * @since 8.10
     */
    protected Map<String, SubType> getCoreAllowedSubtypes(Type type) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Collection<String> coreAllowedSubtypes = schemaManager.getAllowedSubTypes(type.getId());
        if (coreAllowedSubtypes == null) {
            // there are no subtypes to take care of
            return Collections.emptyMap();
        }

        Map<String, SubType> res = new HashMap<>();
        Map<String, SubType> subTypes = type.getAllowedSubTypes();
        for (String name : coreAllowedSubtypes) {
            SubType subtype = subTypes.get(name);
            if (subtype == null) {
                res.put(name, new SubType(name, null));
            } else {
                res.put(name, subtype);
            }
        }

        return res;
    }

    protected DocTypeRegistry getCoreRegistry() {
        return (DocTypeRegistry) Framework.getRuntime()
                                          .getComponentManager()
                                          .getExtensionPointRegistry(TypeService.COMPONENT_NAME, TypeService.XP_DOCTYPE)
                                          .orElseThrow(() -> new IllegalArgumentException(
                                                  String.format("Unknown registry for extension point '%s--%s'",
                                                          TypeService.COMPONENT_NAME, TypeService.XP_DOCTYPE)));
    }

    /**
     * @since 8.4
     */
    protected void registerCoreContribution(Context ctx, Element element, String tag) {
        Node st = DOMHelper.getElementNode(element, "subtypes");
        Node dst = DOMHelper.getElementNode(element, "deniedSubtypes");
        if (!element.hasAttribute("id")) {
            return;
        }
        // forward contribution to core registry, build DOM element from scratch
        Document xmlDoc = new DocumentImpl();
        Element root = xmlDoc.createElement("doctype");
        root.setAttribute("name", element.getAttribute("id"));
        maybeCopyAttribute(element, root, "remove");
        maybeCopyAttribute(element, root, "merge");
        maybeCopyAttribute(element, root, "enable");
        if (st != null) {
            copyChildNode(xmlDoc, st, root, "subtypes", "type");
        }
        if (dst != null) {
            copyChildNode(xmlDoc, dst, root, "subtypes-forbidden", "type");
        }
        getCoreRegistry().registerDocumentType(ctx, xCoreType, root, tag);
    }

    protected void maybeCopyAttribute(Element orig, Element target, String name) {
        if (orig.hasAttribute(name)) {
            target.setAttribute(name, orig.getAttribute(name));
        }
    }

    protected void copyChildNode(Document xmlDoc, Node origChild, Element targetParent, String copyNodeName,
            String subNodeName) {
        Element copy = xmlDoc.createElement(copyNodeName);
        targetParent.appendChild(copy);
        NodeList origChildren = origChild.getChildNodes();
        for (int i = 0; i < origChildren.getLength(); i++) {
            Node childNode = origChildren.item(i);
            if (subNodeName.equals(childNode.getNodeName())) {
                cloneChildNode(xmlDoc, childNode, copy);
            }
        }
    }

    protected void cloneChildNode(Document xmlDoc, Node origNode, Element targetParent) {
        Element clone = xmlDoc.createElement(origNode.getNodeName());
        targetParent.appendChild(clone);
        clone.appendChild(xmlDoc.createTextNode(origNode.getTextContent()));
    }

}
