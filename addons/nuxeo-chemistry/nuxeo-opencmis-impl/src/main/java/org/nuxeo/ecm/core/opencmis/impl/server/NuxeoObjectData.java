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
package org.nuxeo.ecm.core.opencmis.impl.server;

import static org.apache.chemistry.opencmis.commons.impl.Constants.RENDITION_NONE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.commons.BasicPermissions;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ChangeEventInfo;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.MutableAce;
import org.apache.chemistry.opencmis.commons.data.MutableAcl;
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
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlEntryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlPrincipalDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PolicyIdListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RenditionDataImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.opencmis.impl.util.ListUtils;
import org.nuxeo.ecm.core.opencmis.impl.util.SimpleImageInfo;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Nuxeo implementation of a CMIS {@link ObjectData}, backed by a {@link DocumentModel}.
 */
public class NuxeoObjectData implements ObjectData {

    public static final String REND_STREAM_ICON = "nuxeo:icon";

    public static final String REND_KIND_CMIS_THUMBNAIL = "cmis:thumbnail";

    public static final String REND_STREAM_RENDITION_PREFIX = "nuxeo:rendition:";

    public static final String REND_KIND_NUXEO_RENDITION = "nuxeo:rendition";

    /**
     * Property to determine whether all renditions provide a computed size and length.
     *
     * @since 7.4
     */
    public static final String RENDITION_COMPUTE_INFO_PROP = "org.nuxeo.cmis.computeRenditionInfo";

    /**
     * Default for {@value #RENDITION_COMPUTE_INFO_PROP}.
     *
     * @since 7.4
     */
    public static final String RENDITION_COMPUTE_INFO_DEFAULT = "false";

    public CmisService service;

    public DocumentModel doc;

    public boolean creation = false; // TODO

    private List<String> propertyIds;

    private Boolean includeAllowableActions;

    private IncludeRelationships includeRelationships;

    private String renditionFilter;

    private Boolean includePolicyIds;

    private Boolean includeAcl;

    private static final BindingsObjectFactory objectFactory = new BindingsObjectFactoryImpl();

    private TypeDefinition type;

    private List<TypeDefinition> secondaryTypes;

    /** type + secondaryTypes */
    private List<TypeDefinition> allTypes;

    private static final int CACHE_MAX_SIZE = 10;

    private static final int DEFAULT_MAX_RENDITIONS = 20;

    /** Cache for Properties objects, which are expensive to create. */
    private Map<String, Properties> propertiesCache = new HashMap<String, Properties>();

    private CallContext callContext;

    private NuxeoCmisService nuxeoCmisService;

    public NuxeoObjectData(CmisService service, DocumentModel doc, String filter, Boolean includeAllowableActions,
            IncludeRelationships includeRelationships, String renditionFilter, Boolean includePolicyIds,
            Boolean includeAcl, ExtensionsData extension) {
        this.service = service;
        this.doc = doc;
        propertyIds = getPropertyIdsFromFilter(filter);
        this.includeAllowableActions = includeAllowableActions;
        this.includeRelationships = includeRelationships;
        this.renditionFilter = renditionFilter;
        this.includePolicyIds = includePolicyIds;
        this.includeAcl = includeAcl;
        nuxeoCmisService = NuxeoCmisService.extractFromCmisService(service);
        type = nuxeoCmisService.getTypeManager().getTypeDefinition(NuxeoTypeHelper.mappedId(doc.getType()));
        secondaryTypes = new ArrayList<>();
        for (String secondaryTypeId : NuxeoPropertyData.getSecondaryTypeIds(doc)) {
            TypeDefinition td = nuxeoCmisService.getTypeManager().getTypeDefinition(secondaryTypeId);
            if (td != null) {
                secondaryTypes.add(td);
            } // else doc has old facet not declared in types anymore, ignore
        }
        allTypes = new ArrayList<>(1 + secondaryTypes.size());
        allTypes.add(type);
        allTypes.addAll(secondaryTypes);
        callContext = nuxeoCmisService.callContext;
    }

    protected NuxeoObjectData(CmisService service, DocumentModel doc) {
        this(service, doc, null, null, null, null, null, null, null);
    }

    public NuxeoObjectData(CmisService service, DocumentModel doc, OperationContext context) {
        this(service, doc, context.getFilterString(), Boolean.valueOf(context.isIncludeAllowableActions()),
                context.getIncludeRelationships(), context.getRenditionFilterString(),
                Boolean.valueOf(context.isIncludePolicies()), Boolean.valueOf(context.isIncludeAcls()), null);
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
        return NuxeoTypeHelper.getBaseTypeId(doc);
    }

    public List<TypeDefinition> getTypeDefinitions() {
        return allTypes;
    }

    @Override
    public Properties getProperties() {
        return getProperties(propertyIds);
    }

    protected Properties getProperties(List<String> propertyIds) {
        // for STAR_FILTER the key is equal to STAR (see limitCacheSize)
        String key = StringUtils.join(propertyIds, ',');
        Properties properties = propertiesCache.get(key);
        if (properties == null) {
            List<PropertyData<?>> props = new ArrayList<PropertyData<?>>();
            for (TypeDefinition t : allTypes) {
                Map<String, PropertyDefinition<?>> propertyDefinitions = t.getPropertyDefinitions();
                for (PropertyDefinition<?> pd : propertyDefinitions.values()) {
                    if (propertyIds == STAR_FILTER || propertyIds.contains(pd.getId())) {
                        props.add((PropertyData<?>) NuxeoPropertyData.construct(this, pd, callContext));
                    }
                }
            }
            properties = objectFactory.createPropertiesData(props);
            limitCacheSize();
            propertiesCache.put(key, properties);
        }
        return properties;
    }

    /** Limits cache size, always keeps STAR filter. */
    protected void limitCacheSize() {
        if (propertiesCache.size() >= CACHE_MAX_SIZE) {
            Properties sf = propertiesCache.get(STAR);
            propertiesCache.clear();
            if (sf != null) {
                propertiesCache.put(STAR, sf);
            }
        }
    }

    public NuxeoPropertyDataBase<?> getProperty(String id) {
        // make use of cache
        return (NuxeoPropertyDataBase<?>) getProperties(STAR_FILTER).getProperties().get(id);
    }

    @Override
    public AllowableActions getAllowableActions() {
        if (!Boolean.TRUE.equals(includeAllowableActions)) {
            return null;
        }
        return getAllowableActions(doc, creation);
    }

    public static AllowableActions getAllowableActions(DocumentModel doc, boolean creation) {
        BaseTypeId baseType = NuxeoTypeHelper.getBaseTypeId(doc);
        boolean isDocument = baseType == BaseTypeId.CMIS_DOCUMENT;
        boolean isFolder = baseType == BaseTypeId.CMIS_FOLDER;
        boolean isRoot = "/".equals(doc.getPathAsString());
        boolean canWrite = creation || doc.getCoreSession().hasPermission(doc.getRef(), SecurityConstants.WRITE);

        Set<Action> set = EnumSet.noneOf(Action.class);
        set.add(Action.CAN_GET_OBJECT_PARENTS);
        set.add(Action.CAN_GET_PROPERTIES);
        if (isFolder) {
            set.add(Action.CAN_GET_DESCENDANTS);
            set.add(Action.CAN_GET_FOLDER_TREE);
            set.add(Action.CAN_GET_CHILDREN);
            if (!isRoot) {
                set.add(Action.CAN_GET_FOLDER_PARENT);
            }
        } else if (isDocument) {
            set.add(Action.CAN_GET_CONTENT_STREAM);
            set.add(Action.CAN_GET_ALL_VERSIONS);
            set.add(Action.CAN_ADD_OBJECT_TO_FOLDER);
            set.add(Action.CAN_REMOVE_OBJECT_FROM_FOLDER);
            if (doc.isCheckedOut()) {
                set.add(Action.CAN_CHECK_IN);
                set.add(Action.CAN_CANCEL_CHECK_OUT);
            } else {
                set.add(Action.CAN_CHECK_OUT);
            }
        }
        if (isFolder || isDocument) {
            set.add(Action.CAN_GET_RENDITIONS);
        }
        if (canWrite) {
            if (isFolder) {
                set.add(Action.CAN_CREATE_DOCUMENT);
                set.add(Action.CAN_CREATE_FOLDER);
                set.add(Action.CAN_CREATE_RELATIONSHIP);
                set.add(Action.CAN_DELETE_TREE);
            } else if (isDocument) {
                set.add(Action.CAN_SET_CONTENT_STREAM);
                set.add(Action.CAN_DELETE_CONTENT_STREAM);
            }
            set.add(Action.CAN_UPDATE_PROPERTIES);
            if (isFolder && !isRoot || isDocument) {
                // Relationships are not fileable
                set.add(Action.CAN_MOVE_OBJECT);
            }
            if (!isRoot) {
                set.add(Action.CAN_DELETE_OBJECT);
            }
        }
        if (Boolean.FALSE.booleanValue()) {
            // TODO
            set.add(Action.CAN_GET_OBJECT_RELATIONSHIPS);
            set.add(Action.CAN_APPLY_POLICY);
            set.add(Action.CAN_REMOVE_POLICY);
            set.add(Action.CAN_GET_APPLIED_POLICIES);
            set.add(Action.CAN_GET_ACL);
            set.add(Action.CAN_APPLY_ACL);
            set.add(Action.CAN_CREATE_ITEM);
        }

        AllowableActionsImpl aa = new AllowableActionsImpl();
        aa.setAllowableActions(set);
        return aa;
    }

    @Override
    public List<RenditionData> getRenditions() {
        if (!needsRenditions(renditionFilter)) {
            return Collections.emptyList();
        }
        return getRenditions(doc, renditionFilter, null, null, callContext);
    }

    public static boolean needsRenditions(String renditionFilter) {
        return !StringUtils.isBlank(renditionFilter) && !RENDITION_NONE.equals(renditionFilter);
    }

    public static List<RenditionData> getRenditions(DocumentModel doc, String renditionFilter, BigInteger maxItems,
            BigInteger skipCount, CallContext callContext) {
        try {
            List<RenditionData> list = new ArrayList<RenditionData>();
            list.addAll(getRenditionServiceRenditions(doc, callContext));
            // rendition filter
            if (!STAR.equals(renditionFilter)) {
                String[] filters = renditionFilter.split(",");
                for (Iterator<RenditionData> it = list.iterator(); it.hasNext();) {
                    RenditionData ren = it.next();
                    boolean keep = false;
                    for (String filter : filters) {
                        if (filter.contains("/")) {
                            // mimetype
                            if (filter.endsWith("/*")) {
                                String typeSlash = filter.substring(0, filter.indexOf('/') + 1);
                                if (ren.getMimeType().startsWith(typeSlash)) {
                                    keep = true;
                                    break;
                                }
                            } else {
                                if (ren.getMimeType().equals(filter)) {
                                    keep = true;
                                    break;
                                }
                            }
                        } else {
                            // kind
                            if (ren.getKind().equals(filter)) {
                                keep = true;
                                break;
                            }
                        }
                    }
                    if (!keep) {
                        it.remove();
                    }
                }
            }
            list = ListUtils.batchList(list, maxItems, skipCount, DEFAULT_MAX_RENDITIONS);
            return list;
        } catch (IOException e) {
            throw new CmisRuntimeException(e.toString(), e);
        }
    }

    /**
     * @deprecated since 7.3. The thumbnail is now a default rendition, see NXP-16662.
     */
    @Deprecated
    protected static List<RenditionData> getIconRendition(DocumentModel doc, CallContext callContext)
            throws IOException {
        String iconPath;
        try {
            iconPath = (String) doc.getPropertyValue(NuxeoTypeHelper.NX_ICON);
        } catch (PropertyException e) {
            iconPath = null;
        }
        InputStream is = getIconStream(iconPath, callContext);
        if (is == null) {
            return Collections.emptyList();
        }
        RenditionDataImpl ren = new RenditionDataImpl();
        ren.setStreamId(REND_STREAM_ICON);
        ren.setKind(REND_KIND_CMIS_THUMBNAIL);
        int slash = iconPath.lastIndexOf('/');
        String filename = slash == -1 ? iconPath : iconPath.substring(slash + 1);
        ren.setTitle(filename);
        SimpleImageInfo info = new SimpleImageInfo(is);
        ren.setBigLength(BigInteger.valueOf(info.getLength()));
        ren.setBigWidth(BigInteger.valueOf(info.getWidth()));
        ren.setBigHeight(BigInteger.valueOf(info.getHeight()));
        ren.setMimeType(info.getMimeType());
        return Collections.<RenditionData> singletonList(ren);
    }

    /**
     * @deprecated since 7.3. The thumbnail is now a default rendition, see NXP-16662.
     */
    @Deprecated
    public static InputStream getIconStream(String iconPath, CallContext context) {
        if (iconPath == null || iconPath.length() == 0) {
            return null;
        }
        if (!iconPath.startsWith("/")) {
            iconPath = '/' + iconPath;
        }
        ServletContext servletContext = (ServletContext) context.get(CallContext.SERVLET_CONTEXT);
        if (servletContext == null) {
            throw new CmisRuntimeException("Cannot get servlet context");
        }
        return servletContext.getResourceAsStream(iconPath);
    }

    protected static List<RenditionData> getRenditionServiceRenditions(DocumentModel doc, CallContext callContext)
            throws IOException {
        RenditionService renditionService = Framework.getService(RenditionService.class);
        List<RenditionDefinition> defs = renditionService.getAvailableRenditionDefinitions(doc);
        List<RenditionData> list = new ArrayList<>(defs.size());
        for (RenditionDefinition def : defs) {
            if (!def.isVisible()) {
                continue;
            }
            RenditionDataImpl ren = new RenditionDataImpl();
            String cmisName = def.getCmisName();
            if (StringUtils.isBlank(cmisName)) {
                cmisName = REND_STREAM_RENDITION_PREFIX + def.getName();
            }
            ren.setStreamId(cmisName);
            String kind = def.getKind();
            ren.setKind(StringUtils.isNotBlank(kind) ? kind : REND_KIND_NUXEO_RENDITION);
            ren.setTitle(def.getLabel());
            ren.setMimeType(def.getContentType());

            boolean computeInfo = Boolean.parseBoolean(
                    Framework.getProperty(RENDITION_COMPUTE_INFO_PROP, RENDITION_COMPUTE_INFO_DEFAULT));
            if (REND_KIND_CMIS_THUMBNAIL.equals(ren.getKind()) || computeInfo) {
                Rendition rendition = renditionService.getRendition(doc, def.getName());
                Blob blob = rendition.getBlob();
                if (blob != null) {
                    ren.setTitle(blob.getFilename());
                    SimpleImageInfo info = new SimpleImageInfo(blob.getStream());
                    ren.setBigLength(BigInteger.valueOf(info.getLength()));
                    ren.setBigWidth(BigInteger.valueOf(info.getWidth()));
                    ren.setBigHeight(BigInteger.valueOf(info.getHeight()));
                    ren.setMimeType(info.getMimeType());
                }
            }
            list.add(ren);
        }
        return list;
    }

    @Override
    public List<ObjectData> getRelationships() {
        return getRelationships(getId(), includeRelationships, nuxeoCmisService);
    }

    public static List<ObjectData> getRelationships(String id, IncludeRelationships includeRelationships,
            NuxeoCmisService service) {
        if (includeRelationships == null || includeRelationships == IncludeRelationships.NONE) {
            return null;
        }
        String statement = "SELECT " + PropertyIds.OBJECT_ID + ", " + PropertyIds.BASE_TYPE_ID + ", "
                + PropertyIds.SOURCE_ID + ", " + PropertyIds.TARGET_ID + " FROM "
                + BaseTypeId.CMIS_RELATIONSHIP.value() + " WHERE ";
        String qid = "'" + id.replace("'", "''") + "'";
        if (includeRelationships != IncludeRelationships.TARGET) {
            statement += PropertyIds.SOURCE_ID + " = " + qid;
        }
        if (includeRelationships == IncludeRelationships.BOTH) {
            statement += " OR ";
        }
        if (includeRelationships != IncludeRelationships.SOURCE) {
            statement += PropertyIds.TARGET_ID + " = " + qid;
        }
        List<ObjectData> list = new ArrayList<ObjectData>();
        IterableQueryResult res = null;
        try {
            Map<String, PropertyDefinition<?>> typeInfo = new HashMap<String, PropertyDefinition<?>>();
            res = service.queryAndFetch(statement, false, typeInfo);
            for (Map<String, Serializable> map : res) {
                list.add(service.makeObjectData(map, typeInfo));
            }
        } finally {
            if (res != null) {
                res.close();
            }
        }
        return list;
    }

    @Override
    public Acl getAcl() {
        if (!Boolean.TRUE.equals(includeAcl)) {
            return null;
        }
        ACP acp = doc.getACP();
        return getAcl(acp, false, nuxeoCmisService);
    }

    protected static Acl getAcl(ACP acp, boolean onlyBasicPermissions, NuxeoCmisService service) {
        if (acp == null) {
            acp = new ACPImpl();
        }
        Boolean exact = Boolean.TRUE;
        List<Ace> aces = new ArrayList<Ace>();
        for (ACL acl : acp.getACLs()) {
            // inherited and non-local ACLs are non-direct
            boolean direct = ACL.LOCAL_ACL.equals(acl.getName());
            Map<String, Set<String>> permissionMap = new LinkedHashMap<>();
            for (ACE ace : acl.getACEs()) {
                boolean denied = ace.isDenied();
                String username = ace.getUsername();
                String permission = ace.getPermission();
                if (denied) {
                    if (SecurityConstants.EVERYONE.equals(username) && SecurityConstants.EVERYTHING.equals(permission)) {
                        permission = NuxeoCmisService.PERMISSION_NOTHING;
                    } else {
                        // we cannot represent this blocking
                        exact = Boolean.FALSE;
                        continue;
                    }
                }
                Set<String> permissions = permissionMap.get(username);
                if (permissions == null) {
                    permissionMap.put(username, permissions = new LinkedHashSet<String>());
                }
                // derive CMIS permission from Nuxeo permissions
                boolean isBasic = false;
                if (service.readPermissions.contains(permission)) { // Read
                    isBasic = true;
                    permissions.add(BasicPermissions.READ);
                }
                if (service.writePermissions.contains(permission)) { // ReadWrite
                    isBasic = true;
                    permissions.add(BasicPermissions.WRITE);
                }
                if (SecurityConstants.EVERYTHING.equals(permission)) {
                    isBasic = true;
                    permissions.add(BasicPermissions.ALL);
                }
                if (!onlyBasicPermissions) {
                    permissions.add(permission);
                } else if (!isBasic) {
                    exact = Boolean.FALSE;
                }
                if (NuxeoCmisService.PERMISSION_NOTHING.equals(permission)) {
                    break;
                }
            }
            for (Entry<String, Set<String>> en : permissionMap.entrySet()) {
                String username = en.getKey();
                Set<String> permissions = en.getValue();
                if (permissions.isEmpty()) {
                    continue;
                }
                MutableAce entry = new AccessControlEntryImpl();
                entry.setPrincipal(new AccessControlPrincipalDataImpl(username));
                entry.setPermissions(new ArrayList<String>(permissions));
                entry.setDirect(direct);
                aces.add(entry);
            }
        }
        MutableAcl result = new AccessControlListImpl();
        result.setAces(aces);
        result.setExact(exact);
        return result;
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
        return null;
        // throw new UnsupportedOperationException();
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> extensions) {
    }

}
