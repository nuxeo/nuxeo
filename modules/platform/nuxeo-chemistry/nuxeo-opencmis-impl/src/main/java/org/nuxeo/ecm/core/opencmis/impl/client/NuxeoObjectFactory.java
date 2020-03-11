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

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ChangeEvent;
import org.apache.chemistry.opencmis.client.api.ChangeEvents;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.PropertyImpl;
import org.apache.chemistry.opencmis.client.runtime.RenditionImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.DocumentTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.FolderTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.PolicyTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.RelationshipTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.SecondaryTypeImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.FolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyHtmlDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIdDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyUriDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.SecondaryTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Factory for {@link NuxeoObject} and its related classes.
 */
public class NuxeoObjectFactory implements ObjectFactory {

    private final NuxeoSession session;

    private static final BindingsObjectFactory of = new BindingsObjectFactoryImpl();

    public NuxeoObjectFactory(NuxeoSession session) {
        this.session = session;
    }

    @Override
    public void initialize(Session session, Map<String, String> parameters) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public RepositoryInfo convertRepositoryInfo(RepositoryInfo repositoryInfo) {
        return repositoryInfo;
    }

    @Override
    public NuxeoObject convertObject(ObjectData data, OperationContext context) {
        if (data == null || data.getProperties() == null || data.getProperties().getProperties() == null) {
            return null;
        }
        // object type
        PropertyData<?> propData = data.getProperties().getProperties().get(PropertyIds.OBJECT_TYPE_ID);
        ObjectType type = session.getTypeDefinition((String) propData.getFirstValue());
        // secondary types
        propData = data.getProperties().getProperties().get(PropertyIds.SECONDARY_OBJECT_TYPE_IDS);
        List<SecondaryType> secondaryTypes;
        if (propData == null) {
            secondaryTypes = Collections.emptyList();
        } else {
            @SuppressWarnings("unchecked")
            List<String> sts = (List<String>) propData.getValues();
            secondaryTypes = new ArrayList<>(sts.size());
            for (String st : sts) {
                secondaryTypes.add((SecondaryType) session.getTypeDefinition(st));
            }
        }
        return NuxeoObject.construct(session, (NuxeoObjectData) data, type, secondaryTypes);
    }

    @Override
    public ObjectType getTypeFromObjectData(ObjectData objectData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Ace createAce(String principal, List<String> permissions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl createAcl(List<Ace> aces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Property<T> createProperty(PropertyDefinition<T> type, List<T> values) {
        return new PropertyImpl<>(type, values);
    }

    @Override
    public ContentStream createContentStream(String filename, long length, String mimetype, InputStream stream) {
        return new ContentStreamImpl(filename, BigInteger.valueOf(length), mimetype, stream);
    }

    @Override
    public ContentStream createContentStream(String filename, long length, String mimetype, InputStream stream,
            boolean partial) {
        // TODO partial
        return createContentStream(filename, length, mimetype, stream);
    }

    @Override
    public Acl convertAces(List<Ace> aces) {
        return aces == null ? null : new AccessControlListImpl(aces);
    }

    @Override
    public ContentStream convertContentStream(ContentStream contentStream) {
        if (contentStream == null) {
            return null;
        }
        long len = contentStream.getLength();
        BigInteger length = len < 0 ? null : BigInteger.valueOf(len);
        return of.createContentStream(contentStream.getFileName(), length, contentStream.getMimeType(),
                contentStream.getStream());
    }

    @Override
    public List<String> convertPolicies(List<Policy> policies) {
        if (policies == null) {
            return null;
        }
        List<String> policyIds = new ArrayList<>(policies.size());
        for (Policy p : policies) {
            policyIds.add(p.getId());
        }
        return policyIds;
    }

    @Override
    public Map<String, Property<?>> convertProperties(ObjectType objectType, Collection<SecondaryType> secondaryTypes,
            Properties properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties convertProperties(Map<String, ?> properties, ObjectType type,
            Collection<SecondaryType> secondaryTypes, Set<Updatability> updatabilityFilter) {
        if (properties == null) {
            return null;
        }
        // TODO secondaryTypes
        // TODO updatabilityFilter
        PropertiesImpl props = new PropertiesImpl();
        for (Entry<String, ?> es : properties.entrySet()) {
            PropertyData<?> prop = convertProperty(es.getKey(), es.getValue(), type);
            props.addProperty(prop);
        }
        return props;
    }

    @SuppressWarnings("unchecked")
    protected static PropertyData<?> convertProperty(String key, Object value, ObjectType type) {
        PropertyDefinition<?> pd = type.getPropertyDefinitions().get(key);
        if (pd == null) {
            throw new IllegalArgumentException("Unknown property '" + key + "' for type: " + type.getId());
        }
        boolean single = pd.getCardinality() == Cardinality.SINGLE;

        List<?> values;
        if (value == null) {
            values = null;
        } else if (value instanceof List<?>) {
            if (single) {
                throw new IllegalArgumentException("Property '" + key + "' is not a multi value property!");
            }
            values = (List<?>) value;
        } else {
            if (!single) {
                throw new IllegalArgumentException("Property '" + key + "' is not a single value property!");
            }
            values = Collections.singletonList(value);
        }
        Object firstValue = values == null ? null : values.get(0);

        if (pd instanceof PropertyStringDefinition) {
            return of.createPropertyStringData(key, (List<String>) values);
        } else if (pd instanceof PropertyIdDefinition) {
            return of.createPropertyIdData(key, (List<String>) values);
        } else if (pd instanceof PropertyHtmlDefinition) {
            return of.createPropertyHtmlData(key, (List<String>) values);
        } else if (pd instanceof PropertyUriDefinition) {
            return of.createPropertyUriData(key, (List<String>) values);
        } else if (pd instanceof PropertyIntegerDefinition) {
            if (firstValue == null) {
                return of.createPropertyIntegerData(key, (List<BigInteger>) null);
            } else if (firstValue instanceof BigInteger) {
                return of.createPropertyIntegerData(key, (List<BigInteger>) values);
            } else if ((firstValue instanceof Byte) || (firstValue instanceof Short) || (firstValue instanceof Integer)
                    || (firstValue instanceof Long)) {
                List<BigInteger> list = new ArrayList<>(values.size());
                for (Object v : values) {
                    list.add(BigInteger.valueOf(((Number) v).longValue()));
                }
                return of.createPropertyIntegerData(key, list);
            } else {
                throw new IllegalArgumentException("Property '" + key + "' is an Integer property");
            }
        } else if (pd instanceof PropertyBooleanDefinition) {
            return of.createPropertyBooleanData(key, (List<Boolean>) values);
        } else if (pd instanceof PropertyDecimalDefinition) {
            return of.createPropertyDecimalData(key, (List<BigDecimal>) values);
        } else if (pd instanceof PropertyDateTimeDefinition) {
            return of.createPropertyDateTimeData(key, (List<GregorianCalendar>) values);
        }
        throw new CmisRuntimeException("Unknown class: " + pd.getClass().getName());
    }

    @Override
    public List<PropertyData<?>> convertQueryProperties(Properties properties) {
        if (properties == null || properties.getProperties() == null) {
            return null;
        }
        return new ArrayList<>(properties.getPropertyList());
    }

    @Override
    public QueryResult convertQueryResult(ObjectData objectData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendition convertRendition(String objectId, RenditionData rendition) {
        if (rendition == null) {
            return null;
        }
        BigInteger rl = rendition.getBigLength();
        BigInteger rh = rendition.getBigHeight();
        BigInteger rw = rendition.getBigWidth();
        long length = rl == null ? -1 : rl.longValue();
        int height = rh == null ? -1 : rh.intValue();
        int width = rw == null ? -1 : rw.intValue();
        return new RenditionImpl(session, objectId, rendition.getStreamId(), rendition.getRenditionDocumentId(),
                rendition.getKind(), length, rendition.getMimeType(), rendition.getTitle(), height, width);
    }

    @Override
    public ObjectType convertTypeDefinition(TypeDefinition typeDefinition) {
        if (typeDefinition instanceof DocumentTypeDefinition) {
            return new DocumentTypeImpl(session, (DocumentTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof FolderTypeDefinition) {
            return new FolderTypeImpl(session, (FolderTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof RelationshipTypeDefinition) {
            return new RelationshipTypeImpl(session, (RelationshipTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof PolicyTypeDefinition) {
            return new PolicyTypeImpl(session, (PolicyTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof SecondaryTypeDefinition) {
            return new SecondaryTypeImpl(session, (SecondaryTypeDefinition) typeDefinition);
        }
        throw new CmisRuntimeException("Unknown base class: " + typeDefinition.getClass().getName());
    }

    @Override
    public ChangeEvent convertChangeEvent(ObjectData objectData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeEvents convertChangeEvents(String changeLogToken, ObjectList objectList) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
