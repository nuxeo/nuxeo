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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.util.AbstractPageFetch;
import org.apache.chemistry.opencmis.client.runtime.util.CollectionIterable;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.definitions.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Live local CMIS Folder, which is backed by a Nuxeo folderish document.
 */
public class NuxeoFolder extends NuxeoFileableObject implements Folder {

    public NuxeoFolder(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        super(session, data, type);
    }

    @Override
    public Document createDocument(Map<String, ?> properties,
            ContentStream contentStream, VersioningState versioningState,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        Properties props;
        if (properties == null) {
            props = null;
        } else {
            // find type
            String typeId = (String) properties.get(PropertyIds.OBJECT_TYPE_ID);
            if (typeId == null) {
                throw new IllegalArgumentException("Missing type");
            }
            ObjectType type = session.getTypeDefinition(typeId);
            if (type == null) {
                throw new IllegalArgumentException("Unknown type: " + typeId);
            }
            props = convertProperties(properties, type);
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
        Acl addAcl = addAces == null ? null
                : new AccessControlListImpl(addAces);
        Acl removeAcl = removeAces == null ? null : new AccessControlListImpl(
                removeAces);
        String id = service.createDocument(getRepositoryId(), props, getId(),
                contentStream, versioningState, policyIds, addAcl, removeAcl,
                null);
        // must now refetch doc
        return (Document) session.getObject(new ObjectIdImpl(id), context);
    }

    /** Converts from an untyped map to a {@link Properties} object. */
    protected Properties convertProperties(Map<String, ?> properties,
            ObjectType type) {
        Map<String, PropertyDefinition<?>> propDefs = type.getPropertyDefinitions();
        PropertiesImpl props = new PropertiesImpl();
        for (Entry<String, ?> es : properties.entrySet()) {
            String key = es.getKey();
            Object value = es.getValue();
            PropertyDefinition<?> pd = propDefs.get(key);
            if (pd == null) {
                throw new IllegalArgumentException("Unknown property '" + key
                        + "' for type: " + type.getId());
            }
            boolean single = pd.getCardinality() == Cardinality.SINGLE;

            List<?> values;
            if (value == null) {
                values = null;
            } else if (value instanceof List<?>) {
                if (single) {
                    throw new IllegalArgumentException("Property '" + key
                            + "' is not a multi value property!");
                }
                values = (List<?>) value;
            } else {
                if (!single) {
                    throw new IllegalArgumentException("Property '" + key
                            + "' is not a single value property!");
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
                    prop = objectFactory.createPropertyIntegerData(key, list);
                } else {
                    throw new IllegalArgumentException("Property '" + key
                            + "' is an Integer property");
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
        return props;
    }

    @Override
    public Document createDocumentFromSource(ObjectId source,
            Map<String, ?> properties, VersioningState versioningState,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder createFolder(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Policy createPolicy(Map<String, ?> properties,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> deleteTree(boolean allVersions, UnfileObject unfile,
            boolean continueOnFailure) {
        FailedToDeleteData failed = service.deleteTree(getRepositoryId(),
                getId(), Boolean.valueOf(allVersions), unfile,
                Boolean.valueOf(continueOnFailure), null);
        if (failed == null || failed.getIds() == null
                || failed.getIds().isEmpty()) {
            return null;
        }
        return failed.getIds();
    }

    @Override
    public List<ObjectType> getAllowedChildObjectTypes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs(OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemIterable<CmisObject> getChildren() {
        return getChildren(session.getDefaultContext());
    }

    @Override
    public ItemIterable<CmisObject> getChildren(OperationContext context) {
        final ObjectFactory objectFactory = session.getObjectFactory();
        final OperationContext ctx = new OperationContextImpl(context);

        AbstractPageFetch<CmisObject> pageFetcher = new AbstractPageFetch<CmisObject>(
                ctx.getMaxItemsPerPage()) {
            @Override
            protected PageFetchResult<CmisObject> fetchPage(long skipCount) {
                List<CmisObject> page = new ArrayList<CmisObject>();
                DocumentModelList children;
                try {
                    children = service.getCoreSession().getChildren(
                            data.doc.getRef());
                } catch (ClientException e) {
                    throw new CmisRuntimeException(e.toString(), e);
                }
                long totalItems = 0;
                long skip = skipCount;
                // TODO orderBy
                for (DocumentModel child : children) {
                    if (service.isFilteredOut(child)) {
                        continue;
                    }
                    totalItems++;
                    if (skip > 0) {
                        skip--;
                        continue;
                    }
                    if (page.size() > maxNumItems) {
                        continue;
                    }
                    NuxeoObjectData data = new NuxeoObjectData(
                            service.getNuxeoRepository(), child, ctx);
                    CmisObject ob = objectFactory.convertObject(data, ctx);
                    page.add(ob);
                }
                return new PageFetchResult<CmisObject>(page,
                        BigInteger.valueOf(totalItems),
                        Boolean.valueOf(totalItems > skipCount + page.size()));
            }
        };
        return new CollectionIterable<CmisObject>(pageFetcher);
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int depth,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
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

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int depth,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
        return data.doc.getPathAsString();
    }

    @Override
    public boolean isRootFolder() {
        return data.doc.getPath().isRoot();
    }

    @Override
    public ItemIterable<Relationship> getRelationships(
            boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, ObjectType type,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
