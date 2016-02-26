/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentModelFunctions.java 30568 2008-02-25 18:52:49Z ogrisel $
 */

package org.nuxeo.ecm.platform.ui.web.tag.fn;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.core.Manager;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryFunctions;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.runtime.api.Framework;

/**
 * Document model functions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author <a href="mailto:og@nuxeo.com">Olivier Grisel</a>
 */
public final class DocumentModelFunctions implements LiveEditConstants {

    private static final Log log = LogFactory.getLog(DocumentModelFunctions.class);

    private static final String JSESSIONID = "JSESSIONID";

    private static final String i18n_prefix = "%i18n";

    private static final String NXEDIT_URL_VIEW_ID = "nxliveedit.faces";

    private static final String NXEDIT_URL_SCHEME = "nxedit";

    private static MimetypeRegistry mimetypeService;

    private static TypeManager typeManagerService;

    private static DirectoryService dirService;

    private static LifeCycleService lifeCycleService;

    // static cache of default viewId per document type shared all among
    // threads
    private static final Map<String, String> defaultViewCache = Collections.synchronizedMap(new HashMap<String, String>());

    // Utility class.
    private DocumentModelFunctions() {
    }

    private static DirectoryService getDirectoryService() {
        if (dirService == null) {
            dirService = DirectoryHelper.getDirectoryService();
        }
        return dirService;
    }

    private static MimetypeRegistry getMimetypeService() {
        if (mimetypeService == null) {
            mimetypeService = Framework.getService(MimetypeRegistry.class);
        }
        return mimetypeService;
    }

    private static TypeManager getTypeManager() {
        if (typeManagerService == null) {
            typeManagerService = Framework.getService(TypeManager.class);
        }
        return typeManagerService;
    }

    private static String getDefaultView(DocumentModel doc) {
        String docType = doc.getType();

        if (defaultViewCache.containsKey(docType)) {
            return defaultViewCache.get(docType);
        } else {
            org.nuxeo.ecm.platform.types.Type type = getTypeManager().getType(docType);
            if (type == null) {
                return null;
            }
            String defaultView = type.getDefaultView();
            defaultViewCache.put(docType, defaultView);
            return defaultView;
        }
    }

    private static LifeCycleService geLifeCycleService() {
        if (lifeCycleService == null) {
            lifeCycleService = NXCore.getLifeCycleService();
            if (lifeCycleService == null) {
                log.error("No Life Cycle service registered");
            }
        }
        return lifeCycleService;
    }

    public static TypeInfo typeInfo(DocumentModel document) {
        if (document != null) {
            return document.getAdapter(TypeInfo.class);
        } else {
            return null;
        }
    }

    public static String typeLabel(DocumentModel document) {
        String label = "";
        if (document != null) {
            TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
            if (typeInfo != null) {
                label = typeInfo.getLabel();
            }
        }
        return label;
    }

    public static String typeView(DocumentModel document, String viewId) {
        String viewValue = "";
        if (document != null) {
            TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
            if (typeInfo != null) {
                viewValue = typeInfo.getView(viewId);
            }
        }
        return viewValue;
    }

    public static String iconPath(DocumentModel document) {
        String iconPath = "";
        if (document != null) {
            try {
                iconPath = (String) document.getProperty("common", "icon");
            } catch (PropertyException e) {
                iconPath = null;
            }
            if (iconPath == null || iconPath.length() == 0 || document.getType().equals("Workspace")) {
                TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
                if (typeInfo != null) {
                    iconPath = typeInfo.getIcon();
                }
            }
        }
        return iconPath;
    }

    public static String iconExpandedPath(DocumentModel document) {
        String iconPath = "";
        if (document != null) {
            try {
                iconPath = (String) document.getProperty("common", "icon-expanded");
            } catch (PropertyException e) {
                iconPath = null;
            }
            if (iconPath == null || iconPath.length() == 0) {
                TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
                if (typeInfo != null) {
                    iconPath = typeInfo.getIconExpanded();
                    // Set to default icon if expanded is not set
                    if (iconPath == null || iconPath.equals("")) {
                        iconPath = iconPath(document);
                    }
                }

            }
        }
        return iconPath;
    }

    public static String bigIconPath(DocumentModel document) {
        String iconPath = "";
        if (document != null) {
            TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
            if (typeInfo != null) {
                iconPath = typeInfo.getBigIcon();
            }
        }
        return iconPath;
    }

    public static String bigIconExpandedPath(DocumentModel document) {
        String iconPath = "";
        if (document != null) {
            TypeInfo typeInfo = document.getAdapter(TypeInfo.class);
            if (typeInfo != null) {
                iconPath = typeInfo.getIconExpanded();
                // Set to default icon if expanded is not set
                if (iconPath == null || iconPath.equals("")) {
                    iconPath = bigIconPath(document);
                }
            }
        }
        return iconPath;
    }

    public static String fileIconPath(Blob blob) {
        String iconPath = "";
        if (blob != null) {
            MimetypeEntry mimeEntry = getMimetypeService().getMimetypeEntryByMimeType(blob.getMimeType());
            if (mimeEntry != null) {
                if (mimeEntry.getIconPath() != null) {
                    // FIXME: above Context should find it
                    iconPath = "/icons/" + mimeEntry.getIconPath();
                }
            }
        }
        return iconPath;
    }

    public static String titleOrId(DocumentModel document) {
        String title = null;
        if (document != null) {
            try {
                title = (String) document.getProperty("dublincore", "title");
            } catch (PropertyException e) {
                title = null;
            }
            if (title == null || title.length() == 0) {
                // handle root document title
                if ("/".equals(document.getPathAsString())) {
                    title = "/";
                } else {
                    title = document.getId();
                }
            }
        }

        if (title == null) {
            title = "<Unknown>";
        }

        if (title.startsWith(i18n_prefix)) {
            String i18nTitle = title.substring(i18n_prefix.length());
            FacesContext context = FacesContext.getCurrentInstance();
            title = ComponentUtils.translate(context, i18nTitle);
        }
        return title;
    }

    /**
     * @since 6.0
     */
    public static String titleFromId(final String documentId) {
        final CoreSession coreSession = (CoreSession) Component.getInstance("documentManager");
        if (StringUtils.isNotBlank(documentId)) {
            try {
                DocumentModel doc = coreSession.getDocument(new IdRef(documentId));
                if (doc != null) {
                    return titleOrId(doc);
                }
            } catch (DocumentNotFoundException e) {
                log.info("Could not find document with id " + documentId);
            }
        }
        return documentId;
    }

    public static boolean isDocumentModel(Object value) {
        return value instanceof DocumentModel;
    }

    public static boolean isDirty(DocumentModel doc) {
        if (doc == null) {
            return false;
        }
        for (DocumentPart part : doc.getParts()) {
            if (part.isDirty()) {
                // check if dirty properties are not empty
                Iterator<Property> props = part.getDirtyChildren();
                if (props != null) {
                    while (props.hasNext()) {
                        Property prop = props.next();
                        Serializable value = prop.getValue();
                        if (value != null) {
                            if (isPropertyValueDirty(value)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    protected static boolean isPropertyValueDirty(Object value) {
        if (value instanceof String) {
            if (!StringUtils.isBlank((String) value)) {
                return true;
            }
        } else if (value instanceof List) {
            List<?> list = (List) value;
            if (!list.isEmpty()) {
                return true;
            }
        } else if (value instanceof Collection) {
            Collection<?> col = (Collection) value;
            if (!col.isEmpty()) {
                return true;
            }
        } else if (value instanceof Object[]) {
            Object[] col = (Object[]) value;
            if (col.length > 0) {
                return true;
            }
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map) value;
            if (map.isEmpty()) {
                return true;
            }
            for (Object mapItem : map.values()) {
                if (isPropertyValueDirty(mapItem)) {
                    return true;
                }
            }
        } else if (value != null) {
            // in any other case, test if object is null (int, calendar,
            // etc...)
            return true;
        }
        return false;
    }

    public static boolean hasPermission(DocumentModel document, String permission) {
        if (document == null) {
            return false;
        }
        CoreSession session = document.getCoreSession();
        if (session == null) {
            session = (CoreSession) Component.getInstance("documentManager", ScopeType.CONVERSATION);
        }
        if (session == null) {
            log.error("Cannot retrieve CoreSession for " + document);
            return false;
        }
        boolean granted = session.hasPermission(document.getRef(), permission);
        return granted;
    }

    /**
     * Returns true if document can be modified.
     * <p>
     * A document can be modified if current user has 'Write' permission on it and document is mutable (no archived
     * version).
     *
     * @param document
     * @return true if document can be modified.
     */
    public static boolean canModify(DocumentModel document) {
        if (document == null) {
            return false;
        }
        return hasPermission(document, SecurityConstants.WRITE) && !document.hasFacet(FacetNames.IMMUTABLE);
    }

    /**
     * Returns the default value for given schema field.
     *
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the default value.
     * @deprecated use defaultValue(propertyName) instead
     */
    @Deprecated
    public static Object defaultValue(String schemaName, String fieldName) {
        Object value = null;
        SchemaManager tm = Framework.getService(SchemaManager.class);
        Schema schema = tm.getSchema(schemaName);
        Field field = schema.getField(fieldName);
        Type type = field.getType();
        if (type.isListType()) {
            Type itemType = ((ListType) type).getFieldType();
            value = itemType.newInstance();
        }
        return value;
    }

    /**
     * Returns the default value for given property name.
     *
     * @param propertyName as xpath
     * @return the default value.
     */
    public static Object defaultValue(String propertyName) {
        SchemaManager tm = Framework.getService(SchemaManager.class);
        Field field = tm.getField(propertyName);
        Object value = null;
        if (field != null) {
            Type type = field.getType();
            if (type.isListType()) {
                Type itemType = ((ListType) type).getFieldType();
                value = itemType.newInstance();
            } else if (type.isComplexType()) {
                value = type.newInstance();
            } else {
                value = field.getDefaultValue();
            }
        }
        return value;
    }

    /**
     * @since 6.0
     */
    public static String fileUrl(String baseURL, String patternName, DocumentModel doc, String blobPropertyName,
            String filename) {
        if (doc == null) {
            return null;
        }
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        Map<String, String> params = new HashMap<String, String>();
        params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY, blobPropertyName);
        params.put(DocumentFileCodec.FILENAME_KEY, filename);
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        // generate url
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        if (patternName == null) {
            patternName = service.getDefaultPatternName();
        }
        return service.getUrlFromDocumentView(patternName, docView, baseURL);
    }

    public static String fileUrl(String patternName, DocumentModel doc, String blobPropertyName, String filename) {
        return fileUrl(BaseURL.getBaseURL(), patternName, doc, blobPropertyName, filename);
    }

    public static String bigFileUrl(DocumentModel doc, String blobPropertyName, String filename) {
        if (doc == null) {
            return null;
        }
        DownloadService downloadService = Framework.getService(DownloadService.class);
        return BaseURL.getBaseURL() + downloadService.getDownloadUrl(doc, blobPropertyName, filename);
    }

    public static String fileDescription(DocumentModel document, String blobPropertyName, String filePropertyName,
            String filename) {
        String fileInfo = "";
        if (document != null) {
            Long blobLength = null;
            try {
                Blob blob = (Blob) document.getPropertyValue(blobPropertyName);
                if (blob != null) {
                    blobLength = blob.getLength();
                }
            } catch (PropertyException e) {
                // no prop by that name with that type
            }
            if (filename == null && filePropertyName != null) {
                try {
                    filename = (String) document.getPropertyValue(filePropertyName);
                } catch (PropertyException e) {
                    // no prop by that name with that type
                }
            }
            if (blobLength != null && filename != null) {
                fileInfo = filename + " [" + Functions.printFileSize(String.valueOf(blobLength)) + "]";
            } else if (blobLength != null) {
                fileInfo = "[" + Functions.printFileSize(String.valueOf(blobLength)) + "]";
            } else if (filename != null) {
                fileInfo = filename;
            }
        }
        return fileInfo;
    }

    /**
     * Convenient method to get the REST URL of a blob inside the <code>Files</code> schema.
     *
     * @param patternName
     * @param doc The document model.
     * @param index index of the element containing the blob. <code>index</code> starts at 0.
     * @param filename The filename of the blob.
     * @return the REST URL for the blob, or <code>null</code> if an error occurred.
     */
    public static String complexFileUrl(String patternName, DocumentModel doc, int index, String filename) {
        return complexFileUrl(patternName, doc, "files:files", index, DEFAULT_SUB_BLOB_FIELD, filename);
    }

    /**
     * Get the REST URL for a blob inside a list of complex type. For instance,
     * <code>http://localhost/nuxeo/nxfile/server/docId/files:files%5B0%5D/file/image.png</code> for the blob property
     * 'file' of the first element inside the 'files:files' list.
     *
     * @param patternName
     * @param doc The document model.
     * @param listElement Element containing a list of complex type.
     * @param index Index of the element containing the blob inside the list. <code>index</code> starts at 0.
     * @param blobPropertyName The property containing the blob.
     * @param filename Filename of the blob.
     * @return the REST URL for the blob, or <code>null</code> if an error occurred.
     */
    public static String complexFileUrl(String patternName, DocumentModel doc, String listElement, int index,
            String blobPropertyName, String filename) {
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        Map<String, String> params = new HashMap<String, String>();

        String fileProperty = getPropertyPath(listElement, index, blobPropertyName);

        params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY, fileProperty);
        params.put(DocumentFileCodec.FILENAME_KEY, filename);
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        // generate url
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        if (patternName == null) {
            patternName = service.getDefaultPatternName();
        }
        return service.getUrlFromDocumentView(patternName, docView, BaseURL.getBaseURL());
    }

    // TODO: add method accepting filename property name (used for edit online)

    public static String documentUrl(DocumentModel doc, HttpServletRequest req) {
        return documentUrl(null, doc, null, null, false);
    }

    public static String documentUrl(DocumentModel doc) {
        return documentUrl(null, doc, null, null, false);
    }

    /**
     * @deprecated since 7.3, use {@link #documentUrl(String, DocumentLocation, String, Map, boolean, String)} instead.
     */
    public static String documentUrl(String patternName, DocumentModel doc, String viewId,
            Map<String, String> parameters, boolean newConversation) {
        return documentUrl(patternName, doc, viewId, parameters, newConversation, (HttpServletRequest) null);
    }

    /**
     * @deprecated since 7.3, use {@link #documentUrl(String, DocumentLocation, String, Map, boolean, String)} instead.
     */
    public static String documentUrl(String patternName, DocumentModel doc, String viewId,
            Map<String, String> parameters, boolean newConversation, HttpServletRequest req) {
        String baseURL = null;
        if (req == null) {
            baseURL = BaseURL.getBaseURL();
        } else {
            baseURL = BaseURL.getBaseURL(req);
        }
        return documentUrl(patternName, doc, viewId, parameters, newConversation, baseURL);
    }

    /**
     * Computes a document URL.
     *
     * @since 7.3
     */
    public static String documentUrl(String patternName, DocumentModel doc, String viewId,
            Map<String, String> parameters, boolean newConversation, String baseURL) {
        DocumentLocation docLoc = new DocumentLocationImpl(doc);
        if (viewId == null || viewId.length() == 0) {
            viewId = getDefaultView(doc);
        }
        parameters = parameters == null ? new HashMap<String, String>() : parameters;

        if (doc.isVersion()) {
            parameters.put("version", "true");
        }
        return documentUrl(patternName, docLoc, viewId, parameters, newConversation, baseURL);
    }

    /**
     * @since 5.7
     */
    public static String documentUrl(String patternName, DocumentLocation docLoc, String viewId,
            Map<String, String> parameters, boolean newConversation, HttpServletRequest req) {
        String baseURL = null;
        if (req == null) {
            baseURL = BaseURL.getBaseURL();
        } else {
            baseURL = BaseURL.getBaseURL(req);
        }
        return documentUrl(patternName, docLoc, viewId, parameters, newConversation, baseURL);
    }

    /**
     * Computes a document URL.
     *
     * @since 7.3
     */
    public static String documentUrl(String patternName, DocumentLocation docLoc, String viewId,
            Map<String, String> parameters, boolean newConversation, String baseURL) {
        DocumentView docView = new DocumentViewImpl(docLoc, viewId, parameters);

        // generate url
        URLPolicyService service = Framework.getService(URLPolicyService.class);
        PathRef pathRef = docLoc.getPathRef();
        if (pathRef != null && !pathRef.toString().contains("/")) {
            // Use "id" pattern for placeless document
            patternName = "id";
        } else if (patternName == null || patternName.length() == 0) {
            patternName = service.getDefaultPatternName();
        }

        String url = service.getUrlFromDocumentView(patternName, docView, baseURL);

        // pass conversation info if needed
        if (!newConversation && url != null) {
            url = RestHelper.addCurrentConversationParameters(url);
        }

        return url;
    }

    /**
     * Computes an URL for a {@code repositoryName} only.
     *
     * @since 5.7
     * @deprecated since 7.3, use {@link #repositoryUrl(String, String, String, Map, boolean, String)} instead.
     */
    public static String repositoryUrl(String patternName, String repositoryName, String viewId,
            Map<String, String> parameters, boolean newConversation) {
        return repositoryUrl(patternName, repositoryName, viewId, parameters, newConversation,
                (HttpServletRequest) null);
    }

    /**
     * Computes an URL for a {@code repositoryName} only.
     *
     * @since 5.7
     * @deprecated since 7.3, use {@link #repositoryUrl(String, String, String, Map, boolean, String)} instead.
     */
    public static String repositoryUrl(String patternName, String repositoryName, String viewId,
            Map<String, String> parameters, boolean newConversation, HttpServletRequest req) {
        DocumentLocation docLoc = new DocumentLocationImpl(repositoryName, null);
        parameters = parameters == null ? new HashMap<String, String>() : parameters;
        return documentUrl(patternName, docLoc, viewId, parameters, newConversation, req);
    }

    /**
     * Computes an URL for a {@code repositoryName} only.
     *
     * @since 7.3
     */
    public static String repositoryUrl(String patternName, String repositoryName, String viewId,
            Map<String, String> parameters, boolean newConversation, String baseURL) {
        DocumentLocation docLoc = new DocumentLocationImpl(repositoryName, null);
        parameters = parameters == null ? new HashMap<String, String>() : parameters;
        return documentUrl(patternName, docLoc, viewId, parameters, newConversation, baseURL);
    }

    protected static void addQueryParameter(StringBuilder sb, String name, String value, boolean isFirst)
            {
        if (isFirst) {
            sb.append("?");
        } else {
            sb.append("&");
        }
        if (value == null) {
            return;
        }
        sb.append(name);
        sb.append("=");
        try {
            sb.append(URLEncoder.encode(value, URL_ENCODE_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Could not encode URL parameter: " + name + "=" + value, e);
        }
    }

    /**
     * Build the nxedit URL for the "edit existing document" use case for a document using the file:content field as
     * Blob holder
     *
     * @return the encoded URL string
     */
    public static String liveEditUrl(DocumentModel doc) {
        return liveEditUrl(doc, DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD, DEFAULT_FILENAME_FIELD);
    }

    /**
     * Build the nxedit URL for the "edit existing document" use case
     *
     * @return the encoded URL string
     */
    public static String liveEditUrl(DocumentModel doc, String schemaName, String blobFieldName,
            String filenameFieldName) {
        if (doc == null) {
            return ""; // JSF DebugUtil.printTree may call this
        }
        StringBuilder queryParamBuilder = new StringBuilder();
        addQueryParameter(queryParamBuilder, ACTION, ACTION_EDIT_DOCUMENT, true);
        addQueryParameter(queryParamBuilder, REPO_ID, doc.getRepositoryName(), false);
        addQueryParameter(queryParamBuilder, DOC_REF, doc.getRef().toString(), false);
        if (schemaName == null || "".equals(schemaName)) {
            // try to extract it from blob field name
            schemaName = DocumentModelUtils.getSchemaName(blobFieldName);
            blobFieldName = DocumentModelUtils.getFieldName(blobFieldName);
            filenameFieldName = DocumentModelUtils.getFieldName(filenameFieldName);
        }
        addQueryParameter(queryParamBuilder, SCHEMA, schemaName, false);
        addQueryParameter(queryParamBuilder, BLOB_FIELD, blobFieldName, false);
        addQueryParameter(queryParamBuilder, FILENAME_FIELD, filenameFieldName, false);
        return buildNxEditUrl(queryParamBuilder.toString());
    }

    /**
     * Build the nxedit URL for the "edit existing document" use case
     *
     * @return the encoded URL string
     */
    public static String complexLiveEditUrl(DocumentModel doc, String listPropertyName, int index,
            String blobPropertyName, String filenamePropertyName) {

        StringBuilder queryParamBuilder = new StringBuilder();
        addQueryParameter(queryParamBuilder, ACTION, ACTION_EDIT_DOCUMENT, true);
        addQueryParameter(queryParamBuilder, REPO_ID, doc.getRepositoryName(), false);
        addQueryParameter(queryParamBuilder, DOC_REF, doc.getRef().toString(), false);
        addQueryParameter(queryParamBuilder, BLOB_PROPERTY_NAME,
                getPropertyPath(listPropertyName, index, blobPropertyName), false);
        addQueryParameter(queryParamBuilder, FILENAME_PROPERTY_NAME,
                getPropertyPath(listPropertyName, index, filenamePropertyName), false);
        return buildNxEditUrl(queryParamBuilder.toString());
    }

    /**
     * Build the nxedit URL for the "create new document" use case with a document using the file:content field as Blob
     * holder
     *
     * @param mimetype the mime type of the newly created document
     * @return the encoded URL string
     */
    public static String liveCreateUrl(String mimetype) {
        return liveCreateUrl(mimetype, DEFAULT_DOCTYPE, DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD, DEFAULT_FILENAME_FIELD);
    }

    /**
     * Build the nxedit URL for the "create new document" use case
     *
     * @param mimetype the mime type of the newly created document
     * @param docType the document type of the document to create
     * @param schemaName the schema of the blob to hold the new attachment
     * @param blobFieldName the field name of the blob to hold the new attachment
     * @param filenameFieldName the field name of the filename of the new attachment
     * @return the encoded URL string
     */
    public static String liveCreateUrl(String mimetype, String docType, String schemaName, String blobFieldName,
            String filenameFieldName) {

        StringBuilder queryParamBuilder = new StringBuilder();
        addQueryParameter(queryParamBuilder, ACTION, ACTION_CREATE_DOCUMENT, true);
        addQueryParameter(queryParamBuilder, MIMETYPE, mimetype, false);
        addQueryParameter(queryParamBuilder, SCHEMA, schemaName, false);
        addQueryParameter(queryParamBuilder, BLOB_FIELD, blobFieldName, false);
        addQueryParameter(queryParamBuilder, FILENAME_FIELD, filenameFieldName, false);
        addQueryParameter(queryParamBuilder, DOC_TYPE, docType, false);
        return buildNxEditUrl(queryParamBuilder.toString());
    }

    /**
     * Build the nxedit URL for the "create new document from template" use case with "File" doc type and "file" schema
     *
     * @param template the document holding the blob to be used as template
     * @return the encoded URL string
     */
    public static String liveCreateFromTemplateUrl(DocumentModel template) {
        return liveCreateFromTemplateUrl(template, DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD, DEFAULT_DOCTYPE, DEFAULT_SCHEMA,
                DEFAULT_BLOB_FIELD, DEFAULT_FILENAME_FIELD);
    }

    /**
     * Build the nxedit URL for the "create new document from template" use case
     *
     * @param template the document holding the blob to be used as template
     * @param templateSchemaName the schema of the blob holding the template
     * @param templateBlobFieldName the field name of the blob holding the template
     * @param docType the document type of the new document to create
     * @param schemaName the schema of the new blob to be saved as attachment
     * @param blobFieldName the field name of the new blob to be saved as attachment
     * @param filenameFieldName the field name of the filename of the attachment
     * @return the encoded URL string
     */
    public static String liveCreateFromTemplateUrl(DocumentModel template, String templateSchemaName,
            String templateBlobFieldName, String docType, String schemaName, String blobFieldName,
            String filenameFieldName) {

        StringBuilder queryParamBuilder = new StringBuilder();
        addQueryParameter(queryParamBuilder, ACTION, ACTION_CREATE_DOCUMENT_FROM_TEMPLATE, true);
        addQueryParameter(queryParamBuilder, TEMPLATE_REPO_ID, template.getRepositoryName(), false);
        addQueryParameter(queryParamBuilder, TEMPLATE_DOC_REF, template.getRef().toString(), false);
        addQueryParameter(queryParamBuilder, TEMPLATE_SCHEMA, templateSchemaName, false);
        addQueryParameter(queryParamBuilder, TEMPLATE_BLOB_FIELD, templateBlobFieldName, false);
        addQueryParameter(queryParamBuilder, SCHEMA, schemaName, false);
        addQueryParameter(queryParamBuilder, BLOB_FIELD, blobFieldName, false);
        addQueryParameter(queryParamBuilder, FILENAME_FIELD, filenameFieldName, false);
        addQueryParameter(queryParamBuilder, DOC_TYPE, docType, false);
        return buildNxEditUrl(queryParamBuilder.toString());
    }

    private static String buildNxEditUrl(String queryParameters) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // build the URL prefix by concatenating nxedit: scheme with the
        // http:// or https:// base URL from the current request context and
        // the LiveEditBoostrapHelper JSF view
        StringBuilder nxeditUrlBuilder = new StringBuilder(NXEDIT_URL_SCHEME);
        nxeditUrlBuilder.append(":");
        nxeditUrlBuilder.append(BaseURL.getBaseURL(request));
        nxeditUrlBuilder.append(NXEDIT_URL_VIEW_ID);

        // add the query parameters them selves
        nxeditUrlBuilder.append(queryParameters);

        // add seam conversation and JSESSION ids
        addQueryParameter(nxeditUrlBuilder, Manager.instance().getConversationIdParameter(),
                Manager.instance().getCurrentConversationId(), false);
        addQueryParameter(nxeditUrlBuilder, JSESSIONID, extractJSessionId(request), false);
        return nxeditUrlBuilder.toString();
    }

    /**
     * Extract the current JSESSIONID value from the request context
     *
     * @param request the current HttpServletRequest request
     * @return the current JSESSIONID string
     */
    public static String extractJSessionId(HttpServletRequest request) {
        if (request.getSession() != null) {
            return request.getSession().getId();
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equalsIgnoreCase("jsessionid")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns the label for given directory and id.
     *
     * @param directoryName the directory name
     * @param id the label id
     * @return the label.
     * @throws DirectoryException
     * @deprecated use {@link DirectoryFunctions#getDirectoryEntry(String, String)}
     */
    @Deprecated
    public static String getLabelFromId(String directoryName, String id) throws DirectoryException {
        if (id == null) {
            return "";
        }
        try (Session directory = getDirectoryService().open(directoryName)) {
            // XXX hack, directory entries have only one datamodel
            DocumentModel documentModel = directory.getEntry(id);
            String schemaName = documentModel.getSchemas()[0];
            return (String) documentModel.getProperty(schemaName, "label");
        } catch (PropertyException e) {
            return "";
        }
    }

    public static String getPropertyPath(String listPropertyName, int index, String subPropertyName) {
        return listPropertyName + "/" + index + "/" + subPropertyName;
    }

    /**
     * Returns all the available transitions given the current state.
     *
     * @param lifeCycleName the Life Cycle name
     * @param currentState the state from which the transitions should start
     * @since 5.4.2
     */
    public static Collection<String> getAvailableLifeCycleTransitions(String lifeCycleName, String currentState)
            throws LifeCycleException {
        LifeCycle lf = geLifeCycleService().getLifeCycleByName(lifeCycleName);
        return lf.getAllowedStateTransitionsFrom(currentState);
    }

    /**
     * Reset default view cache.
     *
     * @since 5.8
     */
    public static void resetDefaultViewCache() {
        defaultViewCache.clear();
    }

}
