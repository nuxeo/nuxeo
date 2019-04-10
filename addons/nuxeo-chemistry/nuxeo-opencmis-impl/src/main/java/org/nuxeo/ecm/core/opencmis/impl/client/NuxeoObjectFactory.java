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

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.runtime.objecttype.DocumentTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.FolderTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.PolicyTypeImpl;
import org.apache.chemistry.opencmis.client.runtime.objecttype.RelationshipTypeImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.api.Ace;
import org.apache.chemistry.opencmis.commons.api.Acl;
import org.apache.chemistry.opencmis.commons.api.AllowableActions;
import org.apache.chemistry.opencmis.commons.api.ContentStream;
import org.apache.chemistry.opencmis.commons.api.DocumentTypeDefinition;
import org.apache.chemistry.opencmis.commons.api.FolderTypeDefinition;
import org.apache.chemistry.opencmis.commons.api.ObjectData;
import org.apache.chemistry.opencmis.commons.api.PolicyTypeDefinition;
import org.apache.chemistry.opencmis.commons.api.Properties;
import org.apache.chemistry.opencmis.commons.api.PropertyData;
import org.apache.chemistry.opencmis.commons.api.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.api.PropertyId;
import org.apache.chemistry.opencmis.commons.api.RelationshipTypeDefinition;
import org.apache.chemistry.opencmis.commons.api.RenditionData;
import org.apache.chemistry.opencmis.commons.api.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 *
 */
public class NuxeoObjectFactory implements ObjectFactory {

    private final NuxeoSession session;

    public NuxeoObjectFactory(NuxeoSession session) {
        this.session = session;
    }

    public CmisObject convertObject(ObjectData data, OperationContext context) {
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

    public ObjectType getTypeFromObjectData(ObjectData objectData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public <T> Property<T> createProperty(PropertyDefinition<?> type, T value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public <T> Property<T> createPropertyMultivalue(PropertyDefinition<?> type,
            List<T> values) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Ace createAce(String principal, List<String> permissions,
            boolean isDirect) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl createAcl(List<Ace> aces, Boolean isExact) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public AllowableActions createAllowableAction(Map<String, Boolean> actions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ContentStream createContentStream(String filename, long length,
            String mimetype, InputStream stream) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl convertAces(List<Ace> aces) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Acl convertAcl(Acl acl) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public AllowableActions convertAllowableActions(
            AllowableActions allowableActions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public ContentStream convertContentStream(ContentStream contentStream) {
        throw new UnsupportedOperationException();
    }

    public List<String> convertPolicies(List<Policy> policies) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Property<?>> convertProperties(ObjectType objectType,
            Properties properties) {
        throw new UnsupportedOperationException();
    }

    public Properties convertProperties(Map<String, ?> properties,
            ObjectType type, Set<Updatability> updatabilityFilter) {
        throw new UnsupportedOperationException();
    }

    public List<PropertyData<?>> convertQueryProperties(Properties properties) {
        throw new UnsupportedOperationException();
    }

    public QueryResult convertQueryResult(ObjectData objectData) {
        throw new UnsupportedOperationException();
    }

    public Rendition convertRendition(String objectId, RenditionData rendition) {
        throw new UnsupportedOperationException();
    }

    public ObjectType convertTypeDefinition(TypeDefinition typeDefinition) {
        if (typeDefinition instanceof DocumentTypeDefinition) {
            // TODO cast shouldn't be needed
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
        } else {
            throw new CmisRuntimeException("Unknown base type: "
                    + typeDefinition.getClass().getName());
        }
    }

}
