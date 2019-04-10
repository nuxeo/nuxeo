/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.TransientCmisObject;
import org.apache.chemistry.opencmis.client.runtime.PropertyImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData;

/**
 * A transient CMIS object for Nuxeo. Stores transient data until a save() is
 * done.
 */
public class NuxeoTransientObject implements TransientCmisObject {

    protected final NuxeoObject object;

    /** Updated properties. */
    protected Map<String, Object> properties;

    protected Boolean markedForDeleteAllVersions;

    public NuxeoTransientObject(NuxeoObject object) {
        this.object = object;
        properties = new HashMap<String, Object>();
    }

    @Override
    public NuxeoObject getCmisObject() {
        return object;
    }

    @Override
    public String getId() {
        return object.getId();
    }

    @Override
    public ObjectType getType() {
        return object.getType();
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        return object.getBaseTypeId();
    }

    @Override
    public ObjectType getBaseType() {
        return object.getBaseType();
    }

    @Override
    public String getName() {
        return getPropertyValue(PropertyIds.NAME);
    }

    @Override
    public String getChangeToken() {
        return getPropertyValue(PropertyIds.CHANGE_TOKEN);
    }

    @Override
    public String getCreatedBy() {
        return getPropertyValue(PropertyIds.CREATED_BY);
    }

    @Override
    public GregorianCalendar getCreationDate() {
        return getPropertyValue(PropertyIds.CREATION_DATE);
    }

    @Override
    public GregorianCalendar getLastModificationDate() {
        return getPropertyValue(PropertyIds.LAST_MODIFICATION_DATE);
    }

    @Override
    public String getLastModifiedBy() {
        return getPropertyValue(PropertyIds.LAST_MODIFIED_BY);
    }

    @Override
    public AllowableActions getAllowableActions() {
        return object.getAllowableActions();
    }

    @Override
    public List<Policy> getPolicies() {
        return object.getPolicies();
    }

    @Override
    public List<Relationship> getRelationships() {
        return object.getRelationships();
    }

    @Override
    public List<Rendition> getRenditions() {
        return object.getRenditions();
    }

    @Override
    public void addAce(String principalId, List<String> permissions,
            AclPropagation aclPropagation) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void removeAce(String principalId, List<String> permissions,
            AclPropagation aclPropagation) {
        throw new CmisNotSupportedException();
    }

    @Override
    public Acl getOriginalAcl() {
        throw new CmisNotSupportedException();
    }

    @Override
    public void applyPolicy(Policy... policyIds) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void removePolicy(Policy... policyIds) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<CmisExtensionElement> getInputExtensions(ExtensionLevel level) {
        return Collections.emptyList();
    }

    @Override
    public List<CmisExtensionElement> getOutputExtensions(ExtensionLevel level) {
        return Collections.emptyList();
    }

    @Override
    public void setOutputExtensions(ExtensionLevel level,
            List<CmisExtensionElement> extensions) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void delete(boolean allVersions) {
        markedForDeleteAllVersions = Boolean.valueOf(allVersions);
    }

    @Override
    public void setName(String name) {
        setPropertyValue(PropertyIds.NAME, name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPropertyValue(String id) {
        if (properties.containsKey(id)) {
            return (T) properties.get(id);
        }
        // following cast needed by Sun javac
        return (T) object.getPropertyValue(id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Property<T> getProperty(String id) {
        // TODO cache Property objects?
        if (properties.containsKey(id)) {
            T value = (T) properties.get(id);
            PropertyDefinition<T> pd = (PropertyDefinition<T>) getType().getPropertyDefinitions().get(
                    id);
            List<T> values;
            if (value == null) {
                values = Collections.emptyList();
            } else if (value instanceof List<?>) {
                values = (List<T>) value;
            } else {
                values = Collections.singletonList((T) value);
            }
            return new PropertyImpl<T>(pd, values);
        }
        return object.getProperty(id);
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> list = new ArrayList<Property<?>>();
        Set<String> todo = new HashSet<String>(properties.keySet());
        for (Property<?> p : object.getProperties()) {
            String id = p.getId();
            // updated properties take precedence
            if (properties.containsKey(id)) {
                p = getProperty(id);
                todo.remove(id);
            }
            list.add(p);
        }
        // other updated properties not already on the object
        for (String id : todo) {
            list.add(getProperty(id));
        }
        return list;
    }

    @Override
    public void setPropertyValue(String id, Object value) {
        PropertyDefinition<?> pd = getType().getPropertyDefinitions().get(id);
        if (pd == null) {
            throw new CmisInvalidArgumentException("Unknown property: " + id);
        }
        if (pd.getUpdatability() == Updatability.READONLY) {
            throw new CmisInvalidArgumentException("Property is read-only: "
                    + id);
        }
        NuxeoPropertyData.validateCMISValue(value, pd);
        properties.put(id, value);
    }

    @Override
    public boolean isMarkedForDelete() {
        return markedForDeleteAllVersions != null;
    }

    @Override
    public boolean isModified() {
        return markedForDeleteAllVersions != null || !properties.isEmpty();
    }

    protected boolean saveDeletes() {
        if (markedForDeleteAllVersions != null) {
            object.service.deleteObject(object.getRepositoryId(), getId(),
                    markedForDeleteAllVersions, null);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ObjectId save() {
        if (saveDeletes()) {
            reset();
            return null;
        }
        object.updateProperties(properties);
        reset();
        return this;
    }

    @Override
    public void reset() {
        properties.clear();
        markedForDeleteAllVersions = null;
    }

    @Override
    public void refreshAndReset() {
        object.refresh();
        reset();
    }
}
