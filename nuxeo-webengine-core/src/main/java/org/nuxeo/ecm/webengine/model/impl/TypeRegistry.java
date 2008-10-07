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
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.ServiceType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeRegistry extends AbstractContributionRegistry<String, TypeDescriptor>{
    
    //TODO: types map is useless - remove it?
    protected Map<String, ObjectTypeImpl> types;
    protected Map<String, ServiceTypeImpl> services;
    protected Map<String, ServiceTypeImpl[]> serviceBindings;
    protected ModuleImpl module;
    protected Class<?> docObjectClass = null;
    
    public TypeRegistry(ModuleImpl module) {
        types = new ConcurrentHashMap<String, ObjectTypeImpl>();
        services = new ConcurrentHashMap<String, ServiceTypeImpl>();
        serviceBindings = new ConcurrentHashMap<String, ServiceTypeImpl[]>();
        this.module = module;
        // register root type
        TypeDescriptor root = new TypeDescriptor(Resource.class, ResourceType.ROOT_TYPE_NAME, null); 
        registerType(root);        
    }
    
    /**
     * @return the engine.
     */
    public ModuleImpl getModule() {
        return module;
    }

    public ResourceType getType(String name) {
        return types.get(name);
    }
    
    public ServiceType getService(String name) {
        return services.get(name);
    }    


    public ServiceType getService(Resource target, String name) {
        ServiceTypeImpl service = services.get(name);
        if (service != null && service.acceptResource(target)) {
            return service;
        }
        return null;
    }
    
    public List<ServiceType> getServices(Resource resource) {
        ArrayList<ServiceType> result = new ArrayList<ServiceType>();
        collectServicesFor(resource, resource.getType(), result);
        return result;
    }

    public List<String> getServiceNames(Resource resource) {
        ArrayList<String> result = new ArrayList<String>();
        collectServiceNamesFor(resource, resource.getType(), result);
        return result;
    }
    
    public List<ServiceType> getEnabledServices(Resource resource) {
        ArrayList<ServiceType> result = new ArrayList<ServiceType>();
        collectEnabledServicesFor(resource, resource.getType(), result);
        return result;
    }

    public List<String> getEnabledServiceNames(Resource resource) {
        ArrayList<String> result = new ArrayList<String>();
        collectEnabledServiceNamesFor(resource, resource.getType(), result);
        return result;
    }
        
    
    protected void collectServicesFor(Resource ctx, ResourceType type, List<ServiceType> result) {
        ServiceType[] services = serviceBindings.get(type.getName());
        if (services != null && services.length > 0) {
            for (int i=0; i<services.length; i++) {
                ServiceType service = services[i];
                if (service.acceptResource(ctx)) {
                    result.add(service);
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectServicesFor(ctx, superType, result);
        }
    }

    protected void collectServiceNamesFor(Resource ctx, ResourceType type, List<String> result) {
        ServiceType[] services = serviceBindings.get(type.getName());
        if (services != null && services.length > 0) {
            for (int i=0; i<services.length; i++) {
                ServiceType service = services[i];
                if (service.acceptResource(ctx)) {
                    result.add(service.getName());
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectServiceNamesFor(ctx, superType, result);
        }
    }

    protected void collectEnabledServicesFor(Resource ctx, ResourceType type, List<ServiceType> result) {
        ServiceType[] services = serviceBindings.get(type.getName());
        if (services != null && services.length > 0) {
            for (int i=0; i<services.length; i++) {
                ServiceType service = services[i];
                if (service.acceptResource(ctx)) {
                    if (service.isEnabled(ctx)) {
                        result.add(service);
                    }
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectEnabledServicesFor(ctx, superType, result);
        }
    }

    protected void collectEnabledServiceNamesFor(Resource ctx, ResourceType type, List<String> result) {
        ServiceType[] services = serviceBindings.get(type.getName());
        if (services != null && services.length > 0) {
            for (int i=0; i<services.length; i++) {
                ServiceType service = services[i];
                if (service.acceptResource(ctx)) {
                    if (service.isEnabled(ctx)) {
                        result.add(service.getName());
                    }
                }
            }
        }
        ResourceType superType = type.getSuperType();
        if (superType != null) {
            collectEnabledServiceNamesFor(ctx, superType, result);
        }
    }

    
    public ResourceType[] getTypes() {
        return types.values().toArray(new ObjectTypeImpl[types.size()]);
    }
    
    public ServiceType[] getServices() {
        return services.values().toArray(new ServiceTypeImpl[services.size()]);
    }
    
    
    public synchronized void registerType(TypeDescriptor td) {
        if (td.superType != null && !types.containsKey(td.superType)) {
            registerMissingSuperTypeIfNeeded(td);
        }
        addFragment(td.name, td, td.superType);
    }
    
    public synchronized void registerService(ServiceDescriptor td) {
        addFragment(td.name, td, td.superType);
    }
    
    public void unregisterType(TypeDescriptor td) {
        removeFragment(td.name, td);
    }

    public void unregisterService(TypeDescriptor td) {
        removeFragment(td.name, td);
    }
    
    protected void registerMissingSuperTypeIfNeeded(TypeDescriptor td) {
        // we have a special case for document types. 
        // If a web document type is not resolved then use a default web document type
        // This avoid defining web types for every document type in the system. 
        // The web document type use by default the same type hierarchy as document types
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        if (mgr != null) {
            DocumentType doctype = mgr.getDocumentType(td.superType);
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
                    TypeDescriptor superWebType = new TypeDescriptor(docObjectClass, td.superType, superSuperTypeName);
                    registerType(superWebType);                    
                } catch (ClassNotFoundException e) {
                    //TODO
                    System.err.println("Cannot find document resource class. Automatic Core Type support will be disabled ");
                }
            }
        }
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
        if (object.isService()) {
            ServiceDescriptor so = (ServiceDescriptor)object;
            ServiceDescriptor sf = (ServiceDescriptor)fragment;
            if (sf.facets != null && sf.facets.length > 0) {
                ArrayList<String> list = new ArrayList<String>();
                if (so.facets != null && so.facets.length > 0) {
                    list.addAll(Arrays.asList(so.facets));    
                }
                list.addAll(Arrays.asList(sf.facets));
            }
            if (sf.targetTypes != null && sf.targetTypes.length > 0) {
                ArrayList<String> list = new ArrayList<String>();
                if (so.targetTypes != null && so.targetTypes.length > 0) {
                    list.addAll(Arrays.asList(so.targetTypes));    
                }
                list.addAll(Arrays.asList(sf.targetTypes));
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
        if (object.isService()) {
            installServiceContribution(key, (ServiceDescriptor)object);
        } else {
            installTypeContribution(key, object);
        }
    }
    
    protected void installTypeContribution(String key, TypeDescriptor object) {        
        ObjectTypeImpl type = new ObjectTypeImpl(module, null, object.name, (Class<Resource>)object.clazz);
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
        types.put(object.name, type);  
    }

    protected void installServiceContribution(String key, ServiceDescriptor object) {
        ServiceTypeImpl type = new ServiceTypeImpl(module, null, object.name, (Class<Resource>)object.clazz);
        if (object.superType != null) {
            type.superType = types.get(object.superType);
            assert type.superType != null; // must never be null since the object is resolved 
        }
        services.put(object.name, type);  
        // install bindings
        if (object.targetTypes != null && object.targetTypes.length > 0) {
            installServiceBindings(type, object.targetTypes);
        }
    }
    
    protected void installServiceBindings(ServiceTypeImpl service, String ... targetTypes) {
        for (String t : targetTypes) {
            ServiceTypeImpl[] bindings = serviceBindings.get(t);
            if (bindings == null) {
                bindings = new ServiceTypeImpl[] {service};
            } else {
                ServiceTypeImpl[] ar = new ServiceTypeImpl[bindings.length+1];
                System.arraycopy(bindings, 0, ar, 0, bindings.length);
                ar[bindings.length] = service;
            }
            serviceBindings.put(t, bindings);
        }
    }
    
    
    @Override
    protected void updateContribution(String key, TypeDescriptor object) {
          if (object.isService()) {
              updateServiceContribution(key, (ServiceDescriptor)object);
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
        ObjectTypeImpl t = types.get(key);
        if (t != null) { // update the type class
            t.clazz = (Class<Resource>)object.clazz;
            t.loadAnnotations(module.getEngine().getAnnotationManager());
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating an object type which is not registered.");
        }
    }

    protected void updateServiceContribution(String key, ServiceDescriptor object) {
        ObjectTypeImpl t = types.get(key);
        if (t instanceof ServiceTypeImpl) { // update the type class
            ServiceTypeImpl service = (ServiceTypeImpl)t;
            String[] targetTypes = service.targetTypes;
            service.clazz = (Class<Resource>)object.clazz;
            service.loadAnnotations(module.getEngine().getAnnotationManager());
            // update bindings
            if (service.targetTypes != targetTypes) {
                if (!Arrays.equals(targetTypes, service.targetTypes)) {
                    if (targetTypes != null && targetTypes.length > 0) {
                        removeServiceBindings(key, service);
                    }
                    if (service.targetTypes != null && service.targetTypes.length > 0) {
                        installServiceBindings(service, service.targetTypes);
                    }
                }
            }            
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating a service type which is not registered.");
        }        
    }

    
    @Override
    protected void uninstallContribution(String key) {
        //TODO use "@" prefix for services in fragment registry?
        ObjectTypeImpl t = types.remove(key);
        if (t == null) {
            ServiceTypeImpl s = services.remove(key);
            if (s != null) {
                removeServiceBindings(key, (ServiceTypeImpl)t);    
            }
        }
    }
    
    protected void removeServiceBindings(String key, ServiceTypeImpl service) {
        key = key.substring(1);
        if (service.targetTypes != null && service.targetTypes.length > 0) {
            for (String t : service.targetTypes) {
                // remove bindings
                ServiceTypeImpl[] ar = serviceBindings.get(t);
                if (ar != null) {
                    ArrayList<ServiceTypeImpl> list = new ArrayList<ServiceTypeImpl>(ar.length);
                    for (int i=0; i<ar.length; i++) {
                        if (!key.equals(ar[i].getName())) {
                            list.add(ar[i]);
                        }
                    }
                    if (list.isEmpty()) {
                        serviceBindings.remove(key);
                    } else if (list.size() < ar.length) {
                        ar = list.toArray(new ServiceTypeImpl[list.size()]);
                        serviceBindings.put(key, ar);
                    }
                }
            }
        }
    }    
      
    @Override
    protected boolean isMainFragment(TypeDescriptor object) {
        return object.isMainFragment();
    }
            
}
