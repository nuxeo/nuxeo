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
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.PolicyIdList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyIdListImpl;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Nuxeo implementation of ObjectData.
 */
public class NuxeoObjectData implements ObjectData {

    public DocumentModel doc;

    private boolean creation = false; // TODO

    private List<String> propertyIds;

    private Boolean includeAllowableActions;

    private IncludeRelationships includeRelationships;

    private String renditionFilter;

    private Boolean includePolicyIds;

    private Boolean includeAcl;

    private BindingsObjectFactory objectFactory;

    private TypeDefinition type;

    public NuxeoObjectData(NuxeoRepository repository, DocumentModel doc,
            String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter,
            Boolean includePolicyIds, Boolean includeAcl,
            ExtensionsData extension) {
        this.doc = doc;
        propertyIds = getPropertyIdsFromFilter(filter);
        this.includeAllowableActions = includeAllowableActions;
        this.includeRelationships = includeRelationships;
        this.renditionFilter = renditionFilter;
        this.includePolicyIds = includePolicyIds;
        this.includeAcl = includeAcl;
        objectFactory = new BindingsObjectFactoryImpl();
        type = repository.getTypeDefinition(NuxeoTypeHelper.mappedId(doc.getType()));
    }

    private static final String STAR = "*";

    protected static final List<String> STAR_FILTER = Collections.singletonList(STAR);

    protected static List<String> getPropertyIdsFromFilter(String filter) {
        if (filter == null || filter.length() == 0)
            return STAR_FILTER;
        else {
            List<String> ids = Arrays.asList(filter.split(",\\s*"));
            if (ids.contains(STAR)) {
                ids = STAR_FILTER;
            }
            return ids;
        }
    }

    @Override
    public String getId() {
        return doc.getId();
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        return doc.isFolder() ? BaseTypeId.CMIS_FOLDER
                : BaseTypeId.CMIS_DOCUMENT;
    }

    @Override
    public Properties getProperties() {
        return getProperties(propertyIds);
    }

    protected Properties getProperties(List<String> propertyIds) {
        Map<String, PropertyDefinition<?>> propertyDefinitions = type.getPropertyDefinitions();
        int len = propertyIds == STAR_FILTER ? propertyDefinitions.size()
                : propertyIds.size();
        List<PropertyData<?>> props = new ArrayList<PropertyData<?>>(len);
        for (Entry<String, PropertyDefinition<?>> es : propertyDefinitions.entrySet()) {
            String key = es.getKey();
            if (propertyIds == STAR_FILTER || propertyIds.contains(key)) {
                props.add((PropertyData<?>) NuxeoPropertyData.construct(this,
                        es.getValue()));
            }
        }
        return objectFactory.createPropertiesData(props);
    }

    public PropertyData<?> getProperty(String id) {
        PropertyDefinition<?> pd = type.getPropertyDefinitions().get(id);
        return NuxeoPropertyData.construct(this, pd);
    }

    @Override
    public AllowableActions getAllowableActions() {
        if (!Boolean.TRUE.equals(includeAllowableActions)) {
            return null;
        }
        boolean isFolder = doc.isFolder();
        boolean canWrite;
        try {
            canWrite = creation
                    || doc.getCoreSession().hasPermission(doc.getRef(),
                            SecurityConstants.WRITE);
        } catch (ClientException e) {
            canWrite = false;
        }

        Set<Action> set = EnumSet.noneOf(Action.class);
        set.add(Action.CAN_GET_OBJECT_PARENTS);
        set.add(Action.CAN_GET_PROPERTIES);
        if (isFolder) {
            set.add(Action.CAN_GET_DESCENDANTS);
            set.add(Action.CAN_GET_FOLDER_PARENT);
            set.add(Action.CAN_GET_FOLDER_TREE);
            set.add(Action.CAN_GET_CHILDREN);
        } else {
            set.add(Action.CAN_GET_CONTENT_STREAM);
        }
        if (canWrite) {
            if (isFolder) {
                set.add(Action.CAN_CREATE_DOCUMENT);
                set.add(Action.CAN_CREATE_FOLDER);
                set.add(Action.CAN_CREATE_RELATIONSHIP);
                set.add(Action.CAN_DELETE_TREE);
                set.add(Action.CAN_ADD_OBJECT_TO_FOLDER);
                set.add(Action.CAN_REMOVE_OBJECT_FROM_FOLDER);
            } else {
                set.add(Action.CAN_SET_CONTENT_STREAM);
                set.add(Action.CAN_DELETE_CONTENT_STREAM);
            }
            set.add(Action.CAN_UPDATE_PROPERTIES);
            set.add(Action.CAN_MOVE_OBJECT);
            set.add(Action.CAN_DELETE_OBJECT);
        }
        if (Boolean.FALSE.booleanValue()) {
            // TODO
            set.add(Action.CAN_GET_RENDITIONS);
            set.add(Action.CAN_CHECK_OUT);
            set.add(Action.CAN_CANCEL_CHECK_OUT);
            set.add(Action.CAN_CHECK_IN);
            set.add(Action.CAN_GET_ALL_VERSIONS);
            set.add(Action.CAN_GET_OBJECT_RELATIONSHIPS);
            set.add(Action.CAN_APPLY_POLICY);
            set.add(Action.CAN_REMOVE_POLICY);
            set.add(Action.CAN_GET_APPLIED_POLICIES);
            set.add(Action.CAN_GET_ACL);
            set.add(Action.CAN_APPLY_ACL);
        }

        AllowableActionsImpl aa = new AllowableActionsImpl();
        aa.setAllowableActions(set);
        return aa;
    }

    @Override
    public List<RenditionData> getRenditions() {
        if (renditionFilter == null || renditionFilter.length() == 0) {
            return null;
        }
        return new ArrayList<RenditionData>(0); // TODO
    }

    @Override
    public List<ObjectData> getRelationships() {
        if (includeRelationships == null
                || includeRelationships == IncludeRelationships.NONE) {
            return null;
        }
        return new ArrayList<ObjectData>(0); // TODO
    }

    @Override
    public Acl getAcl() {
        if (!Boolean.TRUE.equals(includeAcl)) {
            return null;
        }
        AccessControlListImpl acl = new AccessControlListImpl();
        List<Ace> aces = new ArrayList<Ace>();
        acl.setAces(aces);
        return acl; // TODO
    }

    @Override
    public Boolean isExactAcl() {
        return Boolean.FALSE; // TODO
    }

    @Override
    public PolicyIdList getPolicyIds() {
        if (!Boolean.TRUE.equals(includePolicyIds)) {
            return null;
        }
        return new PolicyIdListImpl(); // TODO
    }

    @Override
    public ChangeEventInfo getChangeEventInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    public void setExtensions(List<Object> extensions) {
    }

}
