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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.PagingIterable;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.api.Ace;
import org.apache.chemistry.opencmis.commons.api.Acl;
import org.apache.chemistry.opencmis.commons.api.AllowableActions;
import org.apache.chemistry.opencmis.commons.api.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.api.ContentStream;
import org.apache.chemistry.opencmis.commons.api.ObjectParentData;
import org.apache.chemistry.opencmis.commons.api.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyData;
import org.apache.chemistry.opencmis.commons.api.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyId;
import org.apache.chemistry.opencmis.commons.api.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Base abstract live local CMIS Object, wrapping a NuxeoSession and a
 * server-side NuxeoObjectData (which is live as well).
 */
public abstract class NuxeoObject implements CmisObject {

    protected final NuxeoSession session;

    public final NuxeoObjectData data;

    private final ObjectType type;

    private static final BindingsObjectFactory objectFactory = new BindingsObjectFactoryImpl();

    public static CmisObject construct(NuxeoSession session,
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
        this.data = data;
        this.type = type;
    }

    public String getRepositoryId() {
        return session.getRepositoryId();
    }

    public String getId() {
        return getPropertyValue(PropertyIds.OBJECT_ID);
    }

    public ObjectType getType() {
        return type;
    }

    public BaseTypeId getBaseTypeId() {
        String baseType = getPropertyValue(PropertyIds.BASE_TYPE_ID);
        return baseType == null ? null : BaseTypeId.fromValue(baseType);
    }

    public ObjectType getBaseType() {
        String baseType = getPropertyValue(PropertyIds.BASE_TYPE_ID);
        return baseType == null ? null : session.getTypeDefinition(baseType);
    }

    public String getName() {
        return getPropertyValue(PropertyIds.NAME);
    }

    public String getChangeToken() {
        return getPropertyValue(PropertyIds.CHANGE_TOKEN);
    }

    public String getCreatedBy() {
        return getPropertyValue(PropertyIds.CREATED_BY);
    }

    public GregorianCalendar getCreationDate() {
        return getPropertyValue(PropertyIds.CREATION_DATE);
    }

    public GregorianCalendar getLastModificationDate() {
        return getPropertyValue(PropertyIds.LAST_MODIFICATION_DATE);
    }

    public String getLastModifiedBy() {
        return getPropertyValue(PropertyIds.LAST_MODIFIED_BY);
    }

    public void setName(String name) {
        setProperty(PropertyIds.NAME, name);
    }

    public void delete(boolean allVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId updateProperties() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ObjectId updateProperties(Map<String, ?> properties) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public <T> T getPropertyValue(String id) {
        return (T) data.getProperty(id).getFirstValue();
    }

    public <T> Property<T> getProperty(String id) {
        return new NuxeoProperty<T>(this, type, id);
    }

    public List<Property<?>> getProperties() {
        Collection<PropertyDefinition<?>> defs = type.getPropertyDefinitions().values();
        List<Property<?>> list = new ArrayList<Property<?>>(defs.size());
        for (PropertyDefinition<?> pd : defs) {
            list.add(new NuxeoProperty<Object>(this, type, pd.getId()));
        }
        return list;
    }

    public <T> void setProperty(String id, T value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public <T> List<T> getPropertyMultivalue(String id) {
        return (List<T>) data.getProperty(id).getValues();
    }

    public <T> void setPropertyMultivalue(String id, List<T> value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void addAcl(List<Ace> addAces, AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl applyAcl(List<Ace> addAces, List<Ace> removeAces,
            AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void applyPolicy(ObjectId policyId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl getAcl() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl getAcl(boolean onlyBasicPermissions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removeAcl(List<Ace> removeAces, AclPropagation aclPropagation) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public AllowableActions getAllowableActions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<Policy> getPolicies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(ObjectId policyId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<Relationship> getRelationships() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public PagingIterable<Relationship> getRelationships(
            boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, ObjectType type,
            OperationContext context, int itemsPerPage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<Rendition> getRenditions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public boolean isChanged() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void refresh() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void refreshIfOld(long durationInMillis) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public long getRefreshTimestamp() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public static abstract class NuxeoFileableObject extends NuxeoObject
            implements FileableCmisObject {

        public NuxeoFileableObject(NuxeoSession session, NuxeoObjectData data,
                ObjectType type) {
            super(session, data, type);
        }

        public void addToFolder(ObjectId folderId, boolean allVersions) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Folder> getParents() {
            String objectId = getId();
            List<ObjectParentData> parentsData = session.getBinding().getNavigationService().getObjectParents(
                    getRepositoryId(), objectId, PropertyIds.OBJECT_ID,
                    Boolean.FALSE, IncludeRelationships.NONE, null,
                    Boolean.FALSE, null);
            List<Folder> parents = new ArrayList<Folder>(parentsData.size());
            for (ObjectParentData p : parentsData) {
                if (p == null || p.getObject() == null
                        || p.getObject().getProperties() == null) {
                    throw new CmisRuntimeException("Invalid object");
                }
                PropertyData<?> idProp = p.getObject().getProperties().getProperties().get(
                        PropertyIds.OBJECT_ID);
                if (!(idProp instanceof PropertyId)) {
                    throw new CmisRuntimeException("Invalid type");
                }
                String id = (String) idProp.getFirstValue();
                CmisObject parent = session.getObject(session.createObjectId(id));
                if (!(parent instanceof Folder)) {
                    throw new CmisRuntimeException("Should be a Folder: "
                            + parent.getClass().getName());
                }
                parents.add((Folder) parent);
            }
            return parents;
        }

        public List<String> getPaths() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public FileableCmisObject move(ObjectId sourceFolderId,
                ObjectId targetFolderId) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void removeFromFolder(ObjectId folderId) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }
    }

    public static class NuxeoFolder extends NuxeoFileableObject implements
            Folder {

        public NuxeoFolder(NuxeoSession session, NuxeoObjectData data,
                ObjectType type) {
            super(session, data, type);
        }

        public Document createDocument(Map<String, ?> properties,
                ContentStream contentStream, VersioningState versioningState,
                List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
                OperationContext context) {
            PropertiesImpl props;
            if (properties == null) {
                props = null;
            } else {
                // find type
                String typeId = (String) properties.get(PropertyIds.OBJECT_TYPE_ID);
                if (typeId == null) {
                    throw new IllegalArgumentException("Missing type");
                }
                ObjectType newType = session.getTypeDefinition(typeId);
                if (newType == null) {
                    throw new IllegalArgumentException("Unknown type: "
                            + typeId);
                }
                Map<String, PropertyDefinition<?>> propertyDefinitions = newType.getPropertyDefinitions();

                props = new PropertiesImpl();
                for (Entry<String, ?> es : properties.entrySet()) {
                    String key = es.getKey();
                    Object value = es.getValue();

                    PropertyDefinition<?> pd = propertyDefinitions.get(key);
                    if (pd == null) {
                        throw new IllegalArgumentException("Unknown property '"
                                + key + "' for type: " + typeId);
                    }
                    boolean single = pd.getCardinality() == Cardinality.SINGLE;

                    List<?> values;
                    if (value == null) {
                        values = null;
                    } else if (value instanceof List<?>) {
                        if (single) {
                            throw new IllegalArgumentException("Property '"
                                    + key + "' is not a multi value property!");
                        }
                        values = (List<?>) value;
                    } else {
                        if (!single) {
                            throw new IllegalArgumentException("Property '"
                                    + key + "' is not a single value property!");
                        }
                        values = Collections.singletonList(value);
                    }
                    Object firstValue = values == null ? null : values.get(0);

                    PropertyData<?> prop;
                    if (pd instanceof PropertyStringDefinition) {
                        prop = objectFactory.createPropertyStringData(key,
                                (List<String>) values);
                    } else if (pd instanceof PropertyIdDefinition) {
                        prop = objectFactory.createPropertyIdData(key,
                                (List<String>) values);
                    } else if (pd instanceof PropertyHtmlDefinition) {
                        prop = objectFactory.createPropertyHtmlData(key,
                                (List<String>) values);
                    } else if (pd instanceof PropertyUriDefinition) {
                        prop = objectFactory.createPropertyUriData(key,
                                (List<String>) values);
                    } else if (pd instanceof PropertyIntegerDefinition) {
                        if (firstValue == null) {
                            prop = objectFactory.createPropertyIntegerData(key,
                                    (List<BigInteger>) null);
                        } else if (firstValue instanceof BigInteger) {
                            prop = objectFactory.createPropertyIntegerData(key,
                                    (List<BigInteger>) values);
                        } else if ((firstValue instanceof Byte)
                                || (firstValue instanceof Short)
                                || (firstValue instanceof Integer)
                                || (firstValue instanceof Long)) {
                            List<BigInteger> list = new ArrayList<BigInteger>(
                                    values.size());
                            for (Object v : values) {
                                list.add(BigInteger.valueOf(((Number) v).longValue()));
                            }
                            prop = objectFactory.createPropertyIntegerData(key,
                                    list);
                        } else {
                            throw new IllegalArgumentException("Property '"
                                    + key + "' is an Integer property");
                        }
                    } else if (pd instanceof PropertyBooleanDefinition) {
                        prop = objectFactory.createPropertyBooleanData(key,
                                (List<Boolean>) values);
                    } else if (pd instanceof PropertyDecimalDefinition) {
                        prop = objectFactory.createPropertyDecimalData(key,
                                (List<BigDecimal>) values);
                    } else if (pd instanceof PropertyDateTimeDefinition) {
                        prop = objectFactory.createPropertyDateTimeData(key,
                                (List<GregorianCalendar>) values);
                    } else {
                        throw new CmisRuntimeException("Unknown class: "
                                + pd.getClass().getName());
                    }

                    props.addProperty(prop);
                }
            }
            List<String> policyIds;
            if (policies == null) {
                policyIds = null;
            } else {
                policyIds = new ArrayList<String>(policies.size());
                for (Policy p : policies) {
                    policyIds.add(p.getId());
                }
            }
            Acl addAcl;
            if (addAces == null) {
                addAcl = null;
            } else {
                addAcl = new AccessControlListImpl(addAces);
            }
            Acl removeAcl;
            if (removeAces == null) {
                removeAcl = null;
            } else {
                removeAcl = new AccessControlListImpl(removeAces);
            }
            String id = session.getBinding().getObjectService().createDocument(
                    getRepositoryId(), props, getId(), contentStream,
                    versioningState, policyIds, addAcl, removeAcl, null);
            // must now refetch doc
            return (Document) session.getObject(new ObjectIdImpl(id), context);
        }

        public Document createDocumentFromSource(ObjectId source,
                Map<String, ?> properties, VersioningState versioningState,
                List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Folder createFolder(Map<String, ?> properties,
                List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Policy createPolicy(Map<String, ?> properties,
                List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<String> deleteTree(boolean allversions,
                UnfileObject unfile, boolean continueOnFailure) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<ObjectType> getAllowedChildObjectTypes() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public PagingIterable<Document> getCheckedOutDocs(int itemsPerPage) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public PagingIterable<Document> getCheckedOutDocs(OperationContext context,
                int itemsPerPage) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public PagingIterable<CmisObject> getChildren(int itemsPerPage) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public PagingIterable<CmisObject> getChildren(OperationContext context,
                int itemsPerPage) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Tree<FileableCmisObject>> getDescendants(int depth) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Tree<FileableCmisObject>> getDescendants(int depth,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Folder getFolderParent() {
            if (isRootFolder()) {
                return null;
            }
            List<Folder> parents = getParents();
            if (parents == null || parents.isEmpty()) {
                return null;
            }
            return parents.get(0);
        }

        public List<Tree<FileableCmisObject>> getFolderTree(int depth) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Tree<FileableCmisObject>> getFolderTree(int depth,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getPath() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public boolean isRootFolder() {
            return session.getRepositoryInfo().getRootFolderId().equals(getId());
        }
    }

    public static class NuxeoDocument extends NuxeoFileableObject implements
            Document {

        public NuxeoDocument(NuxeoSession session, NuxeoObjectData data,
                ObjectType type) {
            super(session, data, type);
        }

        public void cancelCheckOut() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public ObjectId checkIn(boolean major, Map<String, ?> properties,
                ContentStream contentStream, String checkinComment,
                List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public ObjectId checkOut() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Document copy(List<Property<?>> properties,
                VersioningState versioningState, List<Policy> policies,
                List<Ace> addACEs, List<Ace> removeACEs) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public void deleteAllVersions() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public ObjectId deleteContentStream() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Document> getAllVersions() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public List<Document> getAllVersions(OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getCheckinComment() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public ContentStream getContentStream() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getContentStreamFileName() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getContentStreamId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public long getContentStreamLength() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getContentStreamMimeType() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Document getObjectOfLatestVersion(boolean major) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Document getObjectOfLatestVersion(boolean major,
                OperationContext context) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getVersionLabel() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getVersionSeriesCheckedOutBy() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getVersionSeriesCheckedOutId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public String getVersionSeriesId() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Boolean isImmutable() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Boolean isLatestMajorVersion() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Boolean isLatestVersion() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Boolean isMajorVersion() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public Boolean isVersionSeriesCheckedOut() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }

        public ObjectId setContentStream(ContentStream contentStream,
                boolean overwrite) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException();
        }
    }

}
