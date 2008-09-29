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

package org.nuxeo.ecm.webengine.rest.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.webengine.rest.impl.model.DocumentObject;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeRegistry extends AbstractContributionRegistry<String, TypeDescriptorBase>{
    
    //TODO: types map is useless - remove it?
    protected Map<String, DefaultWebType> types;
    
    
    public TypeRegistry() {
        types = new ConcurrentHashMap<String, DefaultWebType>();
        // register root type
        TypeDescriptor root = new TypeDescriptor(WebObject.class, WebType.ROOT_TYPE_NAME, null); 
        registerType(root);        
    }
    

    public WebType getType(String name) {
        return types.get(name);
    }
    
    public WebType[] getTypes() {
        return types.values().toArray(new DefaultWebType[types.size()]);
    }
    
    public void registerAction(ActionDescriptor ad) {
        addFragment(ad.getId(), ad, ad.type);
    }
    
    public void unregisterAction(ActionDescriptor ad) {
        removeFragment(ad.getId(), ad);
    }
    
    public synchronized void registerType(TypeDescriptor td) {
        if (td.superType != null && !types.containsKey(td.superType)) {
            registerMissingSuperTypeIfNeeded(td);
        }
        addFragment(td.name, td, td.superType);
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
                String superSuperTypeName = WebType.ROOT_TYPE_NAME;
                if (superSuperType != null) {
                    superSuperTypeName = superSuperType.getName();
                }
                TypeDescriptor superWebType = new TypeDescriptor(DocumentObject.class, td.superType, superSuperTypeName);
                registerType(superWebType);
            }
        }
    }
    
    public void unregisterType(TypeDescriptor td) {
        removeFragment(td.name, td);
    }
    
    
    @Override
    protected TypeDescriptorBase clone(TypeDescriptorBase object) {
        return object.clone();
    }
    
    @Override
    protected void applyFragment(TypeDescriptorBase object, TypeDescriptorBase fragment) {
        TypeDescriptor td = object.asTypeDescriptor();
        if (td != null) {
            applyTypeFragment(td, fragment.asTypeDescriptor());
        } else {
            applyActionFragment(object.asActionDescriptor(), fragment.asActionDescriptor());
        }
    }

    @Override
    protected void applySuperFragment(TypeDescriptorBase object,
            TypeDescriptorBase superFragment) {
        // do not inherit from parents
    }
    
    @Override
    protected void installContribution(String key, TypeDescriptorBase object) {
        TypeDescriptor td = object.asTypeDescriptor();
        if (td != null) {
            installTypeContribution(key, td);
        } else {
            installActionContribution(key, object.asActionDescriptor());
        }        
    }

    @Override
    protected void uninstallContribution(String key) {
        if (key.indexOf('@') > -1) { // an action
            uninstallActionContribution(key);
        } else {
            uninstallTypeContribution(key);
        }
    }    
    
    @SuppressWarnings("unchecked")
    protected void installTypeContribution(String key, TypeDescriptor td) {
        DefaultWebType type = new DefaultWebType(null, td.name, (Class<WebObject>)td.clazz, td.actions);
        if (td.superType != null) {
            type.superType = types.get(td.superType);
            assert type.superType != null; // must never be null since the object is resolved 
        }
        types.put(td.name, type);
    }
  
    protected void installActionContribution(String key, ActionDescriptor ad) {
        WebType type = types.get(ad.type);
        assert type != null;
        type.addAction(ad);
    }
    
    @Override
    protected boolean isMainFragment(TypeDescriptorBase object) {
        return object.isMainFragment();
    }

    protected void applyTypeFragment(TypeDescriptor object, TypeDescriptor fragment) {
        //TODO for now we don't allow fragments
    }
    
    protected void applyActionFragment(ActionDescriptor object, ActionDescriptor fragment) {
        //TODO
    }

    protected void uninstallActionContribution(String key) {
        int p = key.indexOf('@');
        if (p == -1) {
            throw new IllegalArgumentException("Invalid action key: "+key);
        }
        String action = key.substring(0, p);
        String type = key.substring(p+1);
        DefaultWebType wt = types.get(type);
        if (wt != null) {
            wt.removeAction(action);
        }
    }
    
    protected void uninstallTypeContribution(String key) {
        types.remove(key);
    }


    
}
