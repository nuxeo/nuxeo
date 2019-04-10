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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
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
import org.apache.chemistry.opencmis.client.runtime.PersistentPropertyImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.DocumentTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.FolderTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.PolicyTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.RelationshipTypeImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.definitions.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.FolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Factory for {@link NuxeoObject} and its related classes.
 */
public class NuxeoObjectFactory implements ObjectFactory {

    private final NuxeoSession session;

    public NuxeoObjectFactory(NuxeoSession session) {
        this.session = session;
    }

    @Override
    public NuxeoObject convertObject(ObjectData data, OperationContext context) {
        if (data == null || data.getProperties() == null
                || data.getProperties().getProperties() == null) {
            return null;
        }
        ObjectType type;
        PropertyData<?> propData = data.getProperties().getProperties().get(
                PropertyIds.OBJECT_TYPE_ID);
        if (!(propData instanceof PropertyId)) {
            throw new IllegalArgumentException(
                    "Property cmis:objectTypeId must be of type PropertyIdData, not: "
                            + propData.getClass().getName());
        }
        type = session.getTypeDefinition((String) propData.getFirstValue());
        return NuxeoObject.construct(session, (NuxeoObjectData) data, type);
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
    public <T> Property<T> createProperty(PropertyDefinition<?> type, T value) {
        return new PersistentPropertyImpl<T>(type, value);
    }

    @Override
    public <T> Property<T> createPropertyMultivalue(PropertyDefinition<?> type,
            List<T> values) {
        return new PersistentPropertyImpl<T>(type, values);
    }

    @Override
    public ContentStream createContentStream(String filename, long length,
            String mimetype, InputStream stream) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Acl convertAces(List<Ace> aces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentStream convertContentStream(ContentStream contentStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> convertPolicies(List<Policy> policies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Property<?>> convertProperties(ObjectType objectType,
            Properties properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties convertProperties(Map<String, ?> properties,
            ObjectType type, Set<Updatability> updatabilityFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PropertyData<?>> convertQueryProperties(Properties properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryResult convertQueryResult(ObjectData objectData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendition convertRendition(String objectId, RenditionData rendition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectType convertTypeDefinition(TypeDefinition typeDefinition) {
        if (typeDefinition instanceof DocumentTypeDefinition) {
            return new DocumentTypeImpl(session,
                    (DocumentTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof FolderTypeDefinition) {
            return new FolderTypeImpl(session,
                    (FolderTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof RelationshipTypeDefinition) {
            return new RelationshipTypeImpl(session,
                    (RelationshipTypeDefinition) typeDefinition);
        } else if (typeDefinition instanceof PolicyTypeDefinition) {
            return new PolicyTypeImpl(session,
                    (PolicyTypeDefinition) typeDefinition);
        }
        throw new CmisRuntimeException("Unknown base class: "
                + typeDefinition.getClass().getName());
    }

    @Override
    public ChangeEvent convertChangeEvent(ObjectData objectData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeEvents convertChangeEvents(String changeLogToken,
            ObjectList objectList) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
