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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.webengine.model.ObjectResource;
import org.nuxeo.ecm.webengine.model.ObjectType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeRegistry extends AbstractContributionRegistry<String, TypeDescriptorBase>{
    
    //TODO: types map is useless - remove it?
    protected Map<String, ObjectTypeImpl> types;
    
    
    public TypeRegistry() {
        types = new ConcurrentHashMap<String, ObjectTypeImpl>();
        // register root type
        TypeDescriptor root = new TypeDescriptor(ObjectResource.class, ObjectType.ROOT_TYPE_NAME, null); 
        registerType(root);        
    }
    

    public ObjectType getType(String name) {
        return types.get(name);
    }
    
    public ObjectType[] getTypes() {
        return types.values().toArray(new ObjectTypeImpl[types.size()]);
    }
    
    public void registerAction(ActionTypeImpl ad) {
        addFragment(ad.getId(), ad, ad.type);
    }
    
    public void unregisterAction(ActionTypeImpl ad) {
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
                String superSuperTypeName = ObjectType.ROOT_TYPE_NAME;
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
    protected void updateContribution(String key, TypeDescriptorBase object) {
        TypeDescriptor td = object.asTypeDescriptor();
        if (td != null) {
            updateTypeContribution(key, td);
        } else {
            updateActionContribution(key, object.asActionDescriptor());
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
        ObjectTypeImpl type = new ObjectTypeImpl(null, td.name, (Class<ObjectResource>)td.clazz);
        if (td.superType != null) {
            type.superType = types.get(td.superType);
            assert type.superType != null; // must never be null since the object is resolved 
        }
        types.put(td.name, type);
    }
  
    protected void updateTypeContribution(String key, TypeDescriptor td) {
        // when a type is updated (i.e. reinstalled) we must not replace the existing type since it may contains some contributed actions
        // there are two methods to do this: 
        // 1. update the existing type 
        // 2. unresolve, reinstall then resolve the type contribution to force action reinstalling.
        // we are using 1.
        ObjectTypeImpl t = types.get(key);
        if (t != null) { // update the type class
            t.clazz = (Class<ObjectResource>)td.clazz;
        } else { // install the type - this should never happen since it is an update!
            throw new IllegalStateException("Updating a type which is not registered.");
        }
    }
  
    protected void installActionContribution(String key, ActionTypeImpl ad) {
        ObjectType type = types.get(ad.type);
        assert type != null;
        type.addAction(ad);
    }
  
    protected void updateActionContribution(String key, ActionTypeImpl ad) {
        // no special handling required. updating an action is like re-installing
        installActionContribution(key, ad);
    }
    
    @Override
    protected boolean isMainFragment(TypeDescriptorBase object) {
        return object.isMainFragment();
    }

    protected void applyTypeFragment(TypeDescriptor object, TypeDescriptor fragment) {
        // a type fragment may be used to replace the type implementation class.
        // Super type cannot be replaced 
        if (fragment.clazz != null) {
            object.clazz = fragment.clazz;
        }
    }
    
    protected void applyActionFragment(ActionTypeImpl object, ActionTypeImpl fragment) {
        // for actions we may use fragments to add categories and replace implementation class, 
        // guard and enabled state.
        // Target type cannot be replaced.
        if (fragment.categories != null && !fragment.categories.isEmpty()) {
            object.categories.addAll(fragment.categories);
        }
        if (fragment.clazz != null) {
            object.clazz = fragment.clazz;
        }
        if (object.enabled != fragment.enabled) {
            object.enabled = fragment.enabled;
        }
        if (fragment.guardExpression != null && fragment.guardExpression.length() > 0) {
            object.guardExpression = fragment.guardExpression;
            // TODO use . to path the original expression? 
        }
    }

    protected void uninstallActionContribution(String key) {
        int p = key.indexOf('@');
        if (p == -1) {
            throw new IllegalArgumentException("Invalid action key: "+key);
        }
        String action = key.substring(0, p);
        String type = key.substring(p+1);
        ObjectTypeImpl wt = types.get(type);
        if (wt != null) {
            wt.removeAction(action);
        }
    }
    
    protected void uninstallTypeContribution(String key) {
        types.remove(key);
    }


    
}
