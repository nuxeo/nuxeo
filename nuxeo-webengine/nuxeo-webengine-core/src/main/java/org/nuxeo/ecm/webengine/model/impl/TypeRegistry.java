/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.AdapterType;
import org.nuxeo.ecm.webengine.model.ModuleType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeRegistry extends AbstractContributionRegistry<String, TypeDescriptor>{

    protected final Map<String, AbstractResourceType> types;
    protected final Map<String, AdapterTypeImpl> adapters;
    protected final Map<String, AdapterTypeImpl[]> adapterBindings;
    protected final ModuleImpl module;
    protected Class<?> docObjectClass;

    public TypeRegistry(ModuleImpl module) {
        types = new ConcurrentHashMap<String, AbstractResourceType>();
        adapters = new ConcurrentHashMap<String, AdapterTypeImpl>();
        adapterBindings = new ConcurrentHashMap<String, AdapterTypeImpl[]>();
        this.module = module;
        // register root type
        TypeDescriptor root = new TypeDescriptor(new StaticClassProxy(Resource.class), ResourceType.ROOT_TYPE_NAME, null);
        registerType(root);
        // register module type and its parents if any
        registerModuleType(module);
    }

    protected void registerModuleType(ModuleImpl m) {
        TypeDescriptor td = new ModuleTypeDescriptor(
                new StaticClassProxy(m.descriptor.binding.clazz),
                m.descriptor.name,
                ResourceType.ROOT_TYPE_NAME);
        ModuleImpl sm = m.getSuperModule();
        if (sm != null) {
            registerModuleType(sm);
            td.superType = sm.getName();
        }
        registerType(td);
    }

    public ModuleType getModuleType() {
        return (ModuleType)types.get(module.descriptor.name);
    }

    public ResourceType getRootType() {
        return types.get(ResourceType.ROOT_TYPE_NAME);
    }

    /**
     * @return the engine.
     */
    public ModuleImpl getModule() {
        return module;
    }

    public ResourceType getType(String name) {
        ResourceType type = types.get(name);
        if (type == null) { // check for a non registered document type
            if (registerDocumentTypeIfNeeded(name)) {
                type = types.get(name);
            }
        }
        return type;
    }

    public AdapterType getAdapter(String name) {
        return adapters.get(name);
    }


    public AdapterType getAdapter(Resource target, String name) {
        AdapterTypeImpl adapter = adapters.get(name);
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
        AdapterType[] adapters = adapterBindings.get(type.getName());
        if (adapters != null && adapters.length > 0) {
            for (AdapterType adapter : adapters) {
                if (adapter.acceptResource(ctx)) {
                    result.add(adapter);
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectAdaptersFor(ctx, superType, result);
        }
    }

    protected void collectAdapterNamesFor(Resource ctx, ResourceType type, List<String> result) {
        AdapterType[] adapters = adapterBindings.get(type.getName());
        if (adapters != null && adapters.length > 0) {
            for (AdapterType adapter : adapters) {
                if (adapter.acceptResource(ctx)) {
                    result.add(adapter.getName());
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectAdapterNamesFor(ctx, superType, result);
        }
    }

    protected void collectEnabledAdaptersFor(Resource ctx, ResourceType type, List<AdapterType> result) {
        AdapterType[] adapters = adapterBindings.get(type.getName());
        if (adapters != null && adapters.length > 0) {
            for (AdapterType adapter : adapters) {
                if (adapter.acceptResource(ctx)) {
                    if (adapter.isEnabled(ctx)) {
                        result.add(adapter);
                    }
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectEnabledAdaptersFor(ctx, superType, result);
        }
    }

    protected void collectEnabledAdapterNamesFor(Resource ctx, ResourceType type, List<String> result) {
        AdapterType[] adapters = adapterBindings.get(type.getName());
        if (adapters != null && adapters.length > 0) {
            for (AdapterType adapter : adapters) {
                if (adapter.acceptResource(ctx)) {
                    if (adapter.isEnabled(ctx)) {
                        result.add(adapter.getName());
                    }
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectEnabledAdapterNamesFor(ctx, superType, result);
        }
    }

    public ResourceType[] getTypes() {
        return types.values().toArray(new ResourceTypeImpl[types.size()]);
    }

    public AdapterType[] getAdapters() {
        return adapters.values().toArray(new AdapterTypeImpl[adapters.size()]);
    }

    public synchronized void registerType(TypeDescriptor td) {
        if (td.superType != null && !types.containsKey(td.superType)) {
            registerDocumentTypeIfNeeded(td.superType);
        }
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

    protected boolean registerDocumentTypeIfNeeded(String typeName) {
        // we have a special case for document types.
        // If a web document type is not resolved then use a default web document type
        // This avoid defining web types for every document type in the system.
        // The web document type use by default the same type hierarchy as document types
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        if (mgr != null) {
            DocumentType doctype = mgr.getDocumentType(typeName);
            if (doctype != null) { // this is a document type - register a default web type
                DocumentType superSuperType = (DocumentType)doctype.getSuperType();
                String superSuperTypeName = ResourceType.ROOT_TYPE_NAME;
                if (superSuperType != null) {
                    superSuperTypeName = superSuperType.getName();
                }
                try {
                    if (docObjectClass == null) {
                        docObjectClass = Class.forName("org.nuxeo.ecm.core.rest.DocumentObject");
                    }
                    TypeDescriptor superWebType = new TypeDescriptor(
                            new StaticClassProxy(docObjectClass), typeName, superSuperTypeName);
                    registerType(superWebType);
                    return true;
                } catch (ClassNotFoundException e) {
                    //TODO
                    System.err.println("Cannot find document resource class. Automatic Core Type support will be disabled ");
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
            AdapterDescriptor so = (AdapterDescriptor)object;
            AdapterDescriptor sf = (AdapterDescriptor)fragment;
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
    protected void applySuperFragment(TypeDescriptor object,
            TypeDescriptor superFragment) {
        // do not inherit from parents
    }

    @Override
    protected void installContribution(String key, TypeDescriptor object) {
        if (object.isAdapter()) {
            installAdapterContribution(key, (AdapterDescriptor)object);
        } else {
            installTypeContribution(key, object);
        }
    }

    protected void installTypeContribution(String key, TypeDescriptor object) {
        AbstractResourceType type = null;
        if (object.isModule()) {
            type = new ModuleTypeImpl(module, null, object.type, object.clazz);
        } else {
            type = new ResourceTypeImpl(module, null, object.type, object.clazz);
        }
        if (object.superType != null) {
            type.superType = types.get(object.superType);
            assert type.superType != null; // must never be null since the object is resolved
        }
        // import document facets if this type wraps a document type
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
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
        AdapterTypeImpl type = new AdapterTypeImpl(module, null, object.type, object.name, object.clazz);
        if (object.superType != null) {
            type.superType = types.get(object.superType);
            assert type.superType != null; // must never be null since the object is resolved
        }
        types.put(object.type, type);
        adapters.put(object.name, type);
        // install bindings
        if (object.targetType != null) {
            installAdapterBindings(type, object.targetType);
        }
    }

    protected void installAdapterBindings(AdapterTypeImpl adapter, String targetType) {
        AdapterTypeImpl[] bindings = adapterBindings.get(targetType);
        if (bindings == null) {
            bindings = new AdapterTypeImpl[] {adapter};
        } else {
            AdapterTypeImpl[] ar = new AdapterTypeImpl[bindings.length+1];
            System.arraycopy(bindings, 0, ar, 0, bindings.length);
            ar[bindings.length] = adapter;
        }
        adapterBindings.put(targetType, bindings);
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
     // when a type is updated (i.e. reinstalled) we must not replace the existing type since it may contains some contributed actions
        // there are two methods to do this:
        // 1. update the existing type
        // 2. unresolve, reinstall then resolve the type contribution to force action reinstalling.
        // we are using 1.
        AbstractResourceType t = types.get(key);
        if (t != null) { // update the type class
            t.clazz = object.clazz;
            t.loadAnnotations(module.getEngine().getAnnotationManager());
            t.flushCache();
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating an object type which is not registered.");
        }
    }

    protected void updateAdapterContribution(String key, AdapterDescriptor object) {
        AbstractResourceType t = types.get(key);
        if (t instanceof AdapterTypeImpl) { // update the type class
            AdapterTypeImpl adapter = (AdapterTypeImpl)t;
            String targetType = adapter.targetType;
            adapter.clazz = object.clazz;
            adapter.loadAnnotations(module.getEngine().getAnnotationManager());
            t.flushCache();
            // update bindings
            if (adapter.targetType != null && !adapter.targetType.equals(targetType)) {
                removeAdapterBindings(key, adapter);
                installAdapterBindings(adapter, adapter.targetType);
            }
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating an adapter type which is not registered: "+key);
        }
    }

    @Override
    protected void uninstallContribution(String key, TypeDescriptor value) {
        AbstractResourceType t = types.remove(key);
        if (t instanceof AdapterTypeImpl) {
            AdapterTypeImpl s = adapters.remove(((AdapterTypeImpl)t).name);
            if (s != null) {
                removeAdapterBindings(key, (AdapterTypeImpl)t);
            }
        }
    }

    protected void removeAdapterBindings(String key, AdapterTypeImpl adapter) {
        if (adapter.targetType != null) {
            // remove bindings
            AdapterTypeImpl[] ar = adapterBindings.get(adapter.targetType);
            if (ar != null) {
                ArrayList<AdapterTypeImpl> list = new ArrayList<AdapterTypeImpl>(ar.length);
                for (int i=0; i<ar.length; i++) {
                    if (!key.equals(ar[i].getName())) {
                        list.add(ar[i]);
                    }
                }
                if (list.isEmpty()) {
                    adapterBindings.remove(key);
                } else if (list.size() < ar.length) {
                    ar = list.toArray(new AdapterTypeImpl[list.size()]);
                    adapterBindings.put(key, ar);
                }
            }
        }
    }

    @Override
    protected boolean isMainFragment(TypeDescriptor object) {
        return object.isMainFragment();
    }

}
