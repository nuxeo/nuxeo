/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData.NuxeoPropertyDataName;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyDataBase;

/**
 * Base abstract live local CMIS Object, wrapping a {@link NuxeoSession} and a
 * {@link NuxeoObjectData} which is backed by a Nuxeo document.
 */
public abstract class NuxeoObject implements CmisObject {

    protected static final Set<Updatability> UPDATABILITY_READWRITE = Collections.singleton(Updatability.READWRITE);

    protected final NuxeoSession session;

    protected final NuxeoCmisService service;

    public final NuxeoObjectData data;

    protected final ObjectType type;

    protected static final BindingsObjectFactory objectFactory = new BindingsObjectFactoryImpl();

    public static NuxeoObject construct(NuxeoSession session,
            NuxeoObjectData data, ObjectType type) {
        BaseTypeId baseTypeId = type.getBaseTypeId();
        switch (baseTypeId) {
        case CMIS_FOLDER:
            return new NuxeoFolder(session, data, type);
        case CMIS_DOCUMENT:
            return new NuxeoDocument(session, data, type);
        case CMIS_POLICY:
            throw new UnsupportedOperationException(baseTypeId.toString());
        case CMIS_RELATIONSHIP:
            throw new UnsupportedOperationException(baseTypeId.toString());
        default:
            throw new RuntimeException(baseTypeId.toString());
        }
    }

    public NuxeoObject(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        this.session = session;
        service = session.getService();
        this.data = data;
        this.type = type;
    }

    public String getRepositoryId() {
        return session.getRepositoryId();
    }

    @Override
    public String getId() {
        return data.getId();
    }

    @Override
    public ObjectType getType() {
        return type;
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        return data.getBaseTypeId();
    }

    @Override
    public ObjectType getBaseType() {
        return session.getTypeDefinition(getBaseTypeId().value());
    }

    @Override
    public String getName() {
        return NuxeoPropertyDataName.getValue(data.doc);
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
    public void setName(String name) {
        setProperty(PropertyIds.NAME, name);
    }

    @Override
    public void delete(boolean allVersions) {
        service.deleteObject(getRepositoryId(), getId(),
                Boolean.valueOf(allVersions), null);
    }

    @Override
    public ObjectId updateProperties() {
        CoreSession coreSession = session.getCoreSession();
        try {
            data.doc = coreSession.saveDocument(data.doc);
            coreSession.save();
            return session.createObjectId(data.doc.getId());
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public ObjectId updateProperties(Map<String, ?> properties) {
        for (Entry<String, ?> en : properties.entrySet()) {
            ((NuxeoPropertyDataBase<?>) data.getProperty(en.getKey())).setValue(en.getValue());
        }
        return updateProperties();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPropertyValue(String id) {
        return (T) data.getProperty(id).getValue();
    }

    @Override
    public <T> Property<T> getProperty(String id) {
        return new NuxeoProperty<T>(this, type, id);
    }

    @Override
    public List<Property<?>> getProperties() {
        Collection<PropertyDefinition<?>> defs = type.getPropertyDefinitions().values();
        List<Property<?>> list = new ArrayList<Property<?>>(defs.size());
        for (PropertyDefinition<?> pd : defs) {
            list.add(new NuxeoProperty<Object>(this, type, pd.getId()));
        }
        return list;
    }

    @Override
    public void setProperty(String id, Object value) {
        NuxeoPropertyDataBase<?> prop = data.getProperty(id);
        prop.setValue(value);
    }

    @Override
    public void addAcl(List<Ace> addAces, AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl applyAcl(List<Ace> addAces, List<Ace> removeAces,
            AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyPolicy(ObjectId policyId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl getAcl() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl getAcl(boolean onlyBasicPermissions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAcl(List<Ace> removeAces, AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public AllowableActions getAllowableActions() {
        // we don't call data.getAllowableActions as includeAllowableActions
        // may be false
        return NuxeoObjectData.getAllowableActions(data.doc, data.creation);
    }

    @Override
    public List<Policy> getPolicies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePolicy(ObjectId policyId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Relationship> getRelationships() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ItemIterable<Relationship> getRelationships(
            boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, ObjectType type,
            OperationContext context, int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Rendition> getRenditions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isChanged() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh() {
        try {
            data.doc.refresh();
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public void refreshIfOld(long durationInMillis) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public long getRefreshTimestamp() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
