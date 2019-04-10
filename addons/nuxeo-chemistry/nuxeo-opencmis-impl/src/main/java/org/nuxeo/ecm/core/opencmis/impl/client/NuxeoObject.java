/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData.NuxeoPropertyDataName;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyDataBase;

/**
 * Base abstract live local CMIS Object, wrapping a {@link NuxeoSession} and a {@link NuxeoObjectData} which is backed
 * by a Nuxeo document.
 */
public abstract class NuxeoObject implements CmisObject {

    protected static final Set<Updatability> UPDATABILITY_READWRITE = Collections.singleton(Updatability.READWRITE);

    protected final NuxeoSession session;

    protected final CmisService service;

    protected final NuxeoCmisService nuxeoCmisService;

    protected final NuxeoObjectFactory objectFactory;

    public final NuxeoObjectData data;

    protected final ObjectType type;

    protected final List<SecondaryType> secondaryTypes;

    /** type + secondaryTypes */
    protected final List<ObjectType> allTypes;

    public static NuxeoObject construct(NuxeoSession session, NuxeoObjectData data, ObjectType type,
            List<SecondaryType> secondaryTypes) {
        BaseTypeId baseTypeId = type.getBaseTypeId();
        switch (baseTypeId) {
        case CMIS_FOLDER:
            return new NuxeoFolder(session, data, type, secondaryTypes);
        case CMIS_DOCUMENT:
            return new NuxeoDocument(session, data, type, secondaryTypes);
        case CMIS_POLICY:
            throw new UnsupportedOperationException(baseTypeId.toString());
        case CMIS_RELATIONSHIP:
            return new NuxeoRelationship(session, data, type, secondaryTypes);
        default:
            throw new RuntimeException(baseTypeId.toString());
        }
    }

    public NuxeoObject(NuxeoSession session, NuxeoObjectData data, ObjectType type,
            List<SecondaryType> secondaryTypes) {
        this.session = session;
        service = session.getService();
        nuxeoCmisService = NuxeoCmisService.extractFromCmisService(service);
        objectFactory = session.getObjectFactory();
        this.data = data;
        this.type = type;
        this.secondaryTypes = secondaryTypes;
        allTypes = new ArrayList<>(1 + secondaryTypes.size());
        allTypes.add(type);
        allTypes.addAll(secondaryTypes);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapterInterface) {
        throw new CmisRuntimeException("Cannot adapt to " + adapterInterface.getName());
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
        return secondaryTypes;
    }

    @Override
    public List<ObjectType> findObjectType(String id) {
        List<ObjectType> types = new ArrayList<>(1);
        for (ObjectType t : allTypes) {
            if (t.getPropertyDefinitions().containsKey(id)) {
                types.add(t);
            }
        }
        return types;
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
        service.deleteObject(getRepositoryId(), getId(), Boolean.valueOf(allVersions), null);
    }

    @Override
    public void delete() {
        delete(true);
    }

    @Override
    public CmisObject updateProperties(Map<String, ?> properties) {
        return updateProperties(properties, null, null);
    }

    @Override
    public ObjectId updateProperties(Map<String, ?> properties, boolean refresh) {
        return updateProperties(properties, null, null, refresh);
    }

    @Override
    public CmisObject updateProperties(Map<String, ?> properties, List<String> addSecondaryTypeIds,
            List<String> removeSecondaryTypeIds) {
        ObjectId objectId = updateProperties(properties, addSecondaryTypeIds, removeSecondaryTypeIds, true);
        return session.getObject(objectId);
    }

    @Override
    public ObjectId updateProperties(Map<String, ?> properties, List<String> addSecondaryTypeIds,
            List<String> removeSecondaryTypeIds, boolean refresh) {
        // refresh is ignored
        if (addSecondaryTypeIds != null) {
            for (String facet : addSecondaryTypeIds) {
                data.doc.addFacet(facet);
            }
        }
        if (removeSecondaryTypeIds != null) {
            for (String facet : removeSecondaryTypeIds) {
                data.doc.removeFacet(facet);
            }
        }
        if (properties != null) {
            for (Entry<String, ?> en : properties.entrySet()) {
                ((NuxeoPropertyDataBase<?>) data.getProperty(en.getKey())).setValue(en.getValue());
            }
        }
        CoreSession coreSession = session.getCoreSession();
        data.doc = coreSession.saveDocument(data.doc);
        coreSession.save();
        return this;
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
        return new NuxeoProperty<T>(this, id);
    }

    @Override
    public List<Property<?>> getProperties() {
        List<Property<?>> list = new ArrayList<Property<?>>();
        for (ObjectType t : allTypes) {
            Collection<PropertyDefinition<?>> defs = t.getPropertyDefinitions().values();
            for (PropertyDefinition<?> pd : defs) {
                list.add(new NuxeoProperty<Object>(this, pd.getId()));
            }
        }
        return list;
    }

    @Override
    public Acl addAcl(List<Ace> addAces, AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(), objectFactory.convertAces(addAces), null, aclPropagation,
                null);
    }

    @Override
    public Acl applyAcl(List<Ace> addAces, List<Ace> removeAces, AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(), objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), aclPropagation, null);
    }

    @Override
    public Acl setAcl(List<Ace> aces) {
        return service.applyAcl(getRepositoryId(), getId(), objectFactory.convertAces(aces),
                AclPropagation.REPOSITORYDETERMINED);
    }

    @Override
    public Acl getAcl() {
        return data.getAcl();
    }

    @Override
    public Acl removeAcl(List<Ace> removeAces, AclPropagation aclPropagation) {
        return service.applyAcl(getRepositoryId(), getId(), null, objectFactory.convertAces(removeAces),
                aclPropagation, null);
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
            long length = ren.getBigLength() == null ? -1 : ren.getBigLength().longValue();
            int height = ren.getBigHeight() == null ? -1 : ren.getBigHeight().intValue();
            int width = ren.getBigWidth() == null ? -1 : ren.getBigWidth().intValue();
            RenditionImpl rendition = new RenditionImpl(session, getId(), ren.getStreamId(),
                    ren.getRenditionDocumentId(), ren.getKind(), length, ren.getMimeType(), ren.getTitle(), height,
                    width);
            res.add(rendition);
        }
        return res;
    }

    @Override
    public void refresh() {
        data.doc.refresh();
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
    public Set<String> getPermissionsForPrincipal(String principalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyPolicy(ObjectId policyId, boolean refresh) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePolicy(ObjectId policyId, boolean refresh) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectId> getPolicyIds() {
        return Collections.emptyList();
    }

}
