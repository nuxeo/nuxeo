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
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.runtime.RenditionImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.server.CmisService;
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

    protected final CmisService service;

    protected final NuxeoCmisService nuxeoCmisService;

    protected final NuxeoObjectFactory objectFactory;

    public final NuxeoObjectData data;

    protected final ObjectType type;

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
            return new NuxeoRelationship(session, data, type);
        default:
            throw new RuntimeException(baseTypeId.toString());
        }
    }

    public NuxeoObject(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        this.session = session;
        service = session.getService();
        nuxeoCmisService = NuxeoCmisService.extractFromCmisService(service);
        objectFactory = session.getObjectFactory();
        this.data = data;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapterInterface) {
        throw new CmisRuntimeException("Cannot adapt to "
                + adapterInterface.getName());
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
    public List<SecondaryType> getSecondaryTypes() {
        // TODO secondary types
        return Collections.emptyList();
    }

    @Override
    public List<ObjectType> findObjectType(String id) {
        if (type.getPropertyDefinitions().containsKey(id)) {
            return Collections.singletonList(type);
        }
        // TODO secondary types
        return null;
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
    public String getDescription() {
        return getPropertyValue(PropertyIds.DESCRIPTION);
    }

    @Override
    public void delete(boolean allVersions) {
        service.deleteObject(getRepositoryId(), getId(),
                Boolean.valueOf(allVersions), null);
    }

    @Override
    public void delete() {
        delete(true);
    }

    @Override
    public CmisObject updateProperties(Map<String, ?> properties) {
        ObjectId objectId = updateProperties(properties, true);
        return session.getObject(objectId);
    }

    @Override
    public ObjectId updateProperties(Map<String, ?> properties, boolean refresh) {
        for (Entry<String, ?> en : properties.entrySet()) {
            ((NuxeoPropertyDataBase<?>) data.getProperty(en.getKey())).setValue(en.getValue());
        }
        try {
            CoreSession coreSession = session.getCoreSession();
            data.doc = coreSession.saveDocument(data.doc);
            coreSession.save();
            return this;
        } catch (ClientException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    @Override
    public CmisObject rename(String newName) {
        if (newName == null || newName.length() == 0) {
            throw new IllegalArgumentException("New name must not be empty!");
        }

        Map<String, Object> prop = new HashMap<String, Object>();
        prop.put(PropertyIds.NAME, newName);

        return updateProperties(prop);
    }

    @Override
    public ObjectId rename(String newName, boolean refresh) {
        if (newName == null || newName.length() == 0) {
            throw new IllegalArgumentException("New name must not be empty!");
        }

        Map<String, Object> prop = new HashMap<String, Object>();
        prop.put(PropertyIds.NAME, newName);

        return updateProperties(prop, refresh);
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
    public Acl addAcl(List<Ace> addAces, AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(),
                objectFactory.convertAces(addAces), null, aclPropagation, null);
    }

    @Override
    public Acl applyAcl(List<Ace> addAces, List<Ace> removeAces,
            AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), aclPropagation, null);
    }

    @Override
    public Acl setAcl(List<Ace> aces) {
        return service.applyAcl(getRepositoryId(), getId(),
                objectFactory.convertAces(aces),
                AclPropagation.REPOSITORYDETERMINED);
    }

    @Override
    public Acl getAcl() {
        return data.getAcl();
    }

    @Override
    public Acl removeAcl(List<Ace> removeAces, AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(), null,
                objectFactory.convertAces(removeAces), aclPropagation, null);
    }

    @Override
    public AllowableActions getAllowableActions() {
        // we don't call data.getAllowableActions as includeAllowableActions
        // may be false
        return NuxeoObjectData.getAllowableActions(data.doc, data.creation);
    }

    @Override
    public List<Policy> getPolicies() {
        return Collections.emptyList();
    }

    @Override
    public void applyPolicy(ObjectId... policyIds) {
        throw new CmisNotSupportedException();
    }

    @Override
    public void removePolicy(ObjectId... policyIds) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<Relationship> getRelationships() {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<Rendition> getRenditions() {
        List<RenditionData> renditions = data.getRenditions();
        if (renditions == null) {
            return Collections.emptyList();
        }
        List<Rendition> res = new ArrayList<Rendition>(renditions.size());
        for (RenditionData ren : renditions) {
            long length = ren.getBigLength() == null ? -1
                    : ren.getBigLength().longValue();
            int height = ren.getBigHeight() == null ? -1
                    : ren.getBigHeight().intValue();
            int width = ren.getBigWidth() == null ? -1
                    : ren.getBigWidth().intValue();
            RenditionImpl rendition = new RenditionImpl(session, getId(),
                    ren.getStreamId(), ren.getRenditionDocumentId(),
                    ren.getKind(), length, ren.getMimeType(), ren.getTitle(),
                    height, width);
            res.add(rendition);
        }
        return res;
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

    @Override
    public List<CmisExtensionElement> getExtensions(ExtensionLevel level) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAllowableAction(Action action) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getPermissonsForPrincipal(String principalId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
