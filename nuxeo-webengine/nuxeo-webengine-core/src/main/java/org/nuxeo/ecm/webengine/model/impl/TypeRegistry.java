/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.server.impl.modelapi.annotation.IntrospectionModeller;
import com.sun.jersey.server.spi.component.ResourceComponentConstructor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TypeRegistry extends AbstractContributionRegistry<String, TypeDescriptor> {

    protected final WebEngine engine; // cannot use module.getEngine() since module may be null

    protected final ModuleImpl module;

    protected final Map<String, AbstractResourceType> types;

    protected final Map<String, AdapterType> adapters;

    protected ClassProxy docObjectClass;

    public TypeRegistry(TypeRegistry parent, WebEngine engine, ModuleImpl module) {
        super(parent);
        types = new ConcurrentHashMap<String, AbstractResourceType>();
        adapters = new ConcurrentHashMap<String, AdapterType>();
        this.module = module;
        this.engine = engine;
        // register root type
        if (parent == null) {
            registerRootType();
        } else {
            importParentContributions();
        }
    }

    public TypeRegistry(WebEngine engine, ModuleImpl module) {
        this(null, engine, module);
    }

    protected void registerRootType() {
        TypeDescriptor root = new TypeDescriptor(new StaticClassProxy(ModuleRoot.class), ResourceType.ROOT_TYPE_NAME,
                null);
        registerType(root);
    }

    public ResourceType getRootType() {
        return types.get(ResourceType.ROOT_TYPE_NAME);
    }

    public ModuleImpl getModule() {
        return module;
    }

    public ResourceType getType(String name) {
        return types.get(name);
    }

    public AdapterType getAdapter(String name) {
        return adapters.get(name);
    }

    public AdapterType getAdapter(Resource target, String name) {
        AdapterType adapter = adapters.get(name);
        if (adapter != null && adapter.acceptResource(target)) {
            return adapter;
        }
        return null;
    }

    public List<AdapterType> getAdapters(Resource resource) {
        List<AdapterType> result = new ArrayList<AdapterType>();
        collectAdaptersFor(resource, resource.getType(), result);
        return result;
    }

    public List<String> getAdapterNames(Resource resource) {
        List<String> result = new ArrayList<String>();
        collectAdapterNamesFor(resource, resource.getType(), result);
        return result;
    }

    public List<AdapterType> getEnabledAdapters(Resource resource) {
        List<AdapterType> result = new ArrayList<AdapterType>();
        collectEnabledAdaptersFor(resource, resource.getType(), result);
        return result;
    }

    public List<String> getEnabledAdapterNames(Resource resource) {
        List<String> result = new ArrayList<String>();
        collectEnabledAdapterNamesFor(resource, resource.getType(), result);
        return result;
    }

    protected void collectAdaptersFor(Resource ctx, ResourceType type, List<AdapterType> result) {
        for (AdapterType adapter : getAdapters()) {
            if (adapter.acceptResource(ctx)) {
                result.add(adapter);
            }
        }
    }

    protected void collectAdapterNamesFor(Resource ctx, ResourceType type, List<String> result) {
        for (AdapterType adapter : getAdapters()) {
            if (adapter.acceptResource(ctx)) {
                result.add(adapter.getAdapterName());
            }
        }
    }

    protected void collectEnabledAdaptersFor(Resource ctx, ResourceType type, List<AdapterType> result) {
        for (AdapterType adapter : getAdapters()) {
            if (adapter.acceptResource(ctx) && adapter.isEnabled(ctx)) {
                result.add(adapter);
            }
        }
    }

    protected void collectEnabledAdapterNamesFor(Resource ctx, ResourceType type, List<String> result) {
        for (AdapterType adapter : getAdapters()) {
            if (adapter.acceptResource(ctx) && adapter.isEnabled(ctx)) {
                result.add(adapter.getAdapterName());
            }
        }
    }

    public ResourceType[] getTypes() {
        return types.values().toArray(new ResourceType[types.size()]);
    }

    public AdapterType[] getAdapters() {
        return adapters.values().toArray(new AdapterType[adapters.size()]);
    }

    public void registerTypeDescriptor(TypeDescriptor td) {
        if (td.isAdapter()) {
            registerAdapter(td.asAdapterDescriptor());
        } else {
            registerType(td);
        }
    }

    public synchronized void registerType(TypeDescriptor td) {
        addFragment(td.type, td, td.superType);
    }

    public synchronized void registerAdapter(AdapterDescriptor td) {
        addFragment(td.type, td, td.superType);
    }

    public void unregisterType(TypeDescriptor td) {
        removeFragment(td.type, td);
    }

    public void unregisterAdapter(TypeDescriptor td) {
        removeFragment(td.type, td);
    }

    /**
     * @deprecated since 9.3. Should not be used.
     */
    @Deprecated
    protected boolean registerDocumentTypeIfNeeded(String typeName) {
        // we have a special case for document types.
        // If a web document type is not resolved then use a default web document type
        // This avoid defining web types for every document type in the system.
        // The web document type use by default the same type hierarchy as document types
        SchemaManager mgr = Framework.getService(SchemaManager.class);
        if (mgr != null) {
            DocumentType doctype = mgr.getDocumentType(typeName);
            if (doctype != null) { // this is a document type - register a default web type
                DocumentType superSuperType = (DocumentType) doctype.getSuperType();
                String superSuperTypeName = ResourceType.ROOT_TYPE_NAME;
                if (superSuperType != null) {
                    superSuperTypeName = superSuperType.getName();
                }
                try {
                    if (docObjectClass == null) {
                        docObjectClass = engine.getWebLoader().getClassProxy("org.nuxeo.ecm.core.rest.DocumentObject");
                    }
                    TypeDescriptor superWebType = new TypeDescriptor(docObjectClass, typeName, superSuperTypeName);
                    registerType(superWebType);
                    return true;
                } catch (ClassNotFoundException e) {
                    // TODO
                    System.err.println(
                            "Cannot find document resource class. Automatic Core Type support will be disabled ");
                }
            }
        }
        return false;
    }

    @Override
    protected TypeDescriptor clone(TypeDescriptor object) {
        return object.clone();
    }

    @Override
    protected void applyFragment(TypeDescriptor object, TypeDescriptor fragment) {
        // a type fragment may be used to replace the type implementation class.
        // Super type cannot be replaced
        if (fragment.clazz != null) {
            object.clazz = fragment.clazz;
        }
        if (object.isAdapter()) {
            AdapterDescriptor so = (AdapterDescriptor) object;
            AdapterDescriptor sf = (AdapterDescriptor) fragment;
            if (sf.facets != null && sf.facets.length > 0) {
                List<String> list = new ArrayList<String>();
                if (so.facets != null && so.facets.length > 0) {
                    list.addAll(Arrays.asList(so.facets));
                }
                list.addAll(Arrays.asList(sf.facets));
            }
            if (sf.targetType != null && !sf.targetType.equals(ResourceType.ROOT_TYPE_NAME)) {
                so.targetType = sf.targetType;
            }
        }
    }

    @Override
    protected void applySuperFragment(TypeDescriptor object, TypeDescriptor superFragment) {
        // do not inherit from parents
    }

    @Override
    protected void installContribution(String key, TypeDescriptor object) {
        if (object.isAdapter()) {
            installAdapterContribution(key, (AdapterDescriptor) object);
        } else {
            installTypeContribution(key, object);
        }
    }

    protected void installTypeContribution(String key, TypeDescriptor object) {
        ResourceComponentConstructor constructor = new ResourceComponentConstructor(module.sic, ComponentScope.PerRequest, IntrospectionModeller.createResource(object.clazz.get()));
        AbstractResourceType type = new ResourceTypeImpl(engine, module, null, object.type, object.clazz,
                constructor, object.visibility);
        if (object.superType != null) {
            type.superType = types.get(object.superType);
            assert type.superType != null; // must never be null since the object is resolved
        }
        // import document facets if this type wraps a document type
        SchemaManager mgr = Framework.getService(SchemaManager.class);
        if (mgr != null) {
            DocumentType doctype = mgr.getDocumentType(type.getName());
            if (doctype != null) {
                if (type.facets == null) {
                    type.facets = new HashSet<String>();
                }
                type.facets.addAll(doctype.getFacets());
            }
        }
        // register the type
        types.put(object.type, type);
    }

    protected void installAdapterContribution(String key, AdapterDescriptor object) {
        ResourceComponentConstructor constructor = new ResourceComponentConstructor(module.sic, ComponentScope.PerRequest, IntrospectionModeller.createResource(object.clazz.get()));
        AdapterTypeImpl type = new AdapterTypeImpl(engine, module, null, object.type, object.name, object.clazz, constructor,
                object.visibility);
        if (object.superType != null) {
            type.superType = types.get(object.superType);
            assert type.superType != null; // must never be null since the object is resolved
        }
        types.put(object.type, type);
        adapters.put(object.name, type);
    }

    @Override
    protected void updateContribution(String key, TypeDescriptor object, TypeDescriptor oldValue) {
        if (object.isAdapter()) {
            updateAdapterContribution(key, (AdapterDescriptor) object);
        } else {
            updateTypeContribution(key, object);
        }
    }

    protected void updateTypeContribution(String key, TypeDescriptor object) {
        // When a type is updated (i.e. reinstalled) we must not replace
        // the existing type since it may contains some contributed actions.
        // There are two methods to do this:
        // 1. update the existing type
        // 2. unresolve, reinstall then resolve the type contribution to force action reinstalling.
        // we are using 1.
        AbstractResourceType t = types.get(key);
        if (t != null) { // update the type class
            t.clazz = object.clazz;
            t.loadAnnotations(engine.getAnnotationManager());
            t.flushCache();
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating an object type which is not registered.");
        }
    }

    protected void updateAdapterContribution(String key, AdapterDescriptor object) {
        AbstractResourceType t = types.get(key);
        if (t instanceof AdapterTypeImpl) { // update the type class
            AdapterTypeImpl adapter = (AdapterTypeImpl) t;
            adapter.clazz = object.clazz;
            adapter.loadAnnotations(engine.getAnnotationManager());
            t.flushCache();
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating an adapter type which is not registered: " + key);
        }
    }

    @Override
    protected void uninstallContribution(String key, TypeDescriptor value) {
        AbstractResourceType t = types.remove(key);
        if (t instanceof AdapterTypeImpl) {
            adapters.remove(((AdapterTypeImpl) t).name);
        }
    }

    @Override
    protected boolean isMainFragment(TypeDescriptor object) {
        return object.isMainFragment();
    }

}
