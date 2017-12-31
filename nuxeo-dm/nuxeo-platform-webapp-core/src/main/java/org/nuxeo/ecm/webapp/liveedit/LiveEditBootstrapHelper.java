/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: LiveEditBootstrapHelper.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.webapp.liveedit;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * The LiveEdit bootstrap procedure works as follows:
 * <ul>
 * <li>browsed page calls a JSF function from the DocumentModelFunctions class (edit a document, create new document,
 * etc.) to generate;</li>
 * <li>composing a specific URL as result, triggering the bootstrap addon to popup;</li>
 * <li>the addon come back with the URL composed allowing the present seam component to create the bootstrap file. The
 * file contains various data as requested in the URL;</li>
 * <li>the XML file is now available to addon which presents it to the client plugin.</li>
 * </ul>
 * Please refer to the nuxeo book chapter on desktop integration for details on the format of the nxedit URLs and the
 * XML bootstrap file.
 *
 * @author Thierry Delprat NXP-1959 the bootstrap file is managing the 'create new document [from template]' case too.
 *         The URL is containing an action identifier.
 * @author Rux rdarlea@nuxeo.com
 * @author Olivier Grisel ogrisel@nuxeo.com (split url functions into JSF DocumentModelFunctions module)
 */
@Scope(EVENT)
@Name("liveEditHelper")
public class LiveEditBootstrapHelper implements Serializable, LiveEditConstants {

    protected static final String MODIFIED_FIELD = "modified";

    protected static final String DUBLINCORE_SCHEMA = "dublincore";

    private static final Log log = LogFactory.getLog(LiveEditBootstrapHelper.class);

    private static final long serialVersionUID = 876879071L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @RequestParameter
    protected String action;

    @RequestParameter
    protected String repoID;

    @RequestParameter
    protected String templateRepoID;

    @RequestParameter
    protected String docRef;

    @RequestParameter
    protected String templateDocRef;

    @In(create = true)
    protected LiveEditClientConfig liveEditClientConfig;

    /**
     * @deprecated use blobPropertyField and filenamePropertyField instead
     */
    @Deprecated
    @RequestParameter
    protected String schema;

    @RequestParameter
    protected String templateSchema;

    /**
     * @deprecated use blobPropertyField instead
     */
    @Deprecated
    @RequestParameter
    protected String blobField;

    @RequestParameter
    protected String blobPropertyName;

    @RequestParameter
    protected String templateBlobField;

    // TODO: to be deprecated once all filenames are stored in the blob itself
    /**
     * @deprecated use filenamePropertyField instead
     */
    @Deprecated
    @RequestParameter
    protected String filenameField;

    // TODO: to be deprecated once all filenames are stored in the blob itself
    @RequestParameter
    protected String filenamePropertyName;

    @RequestParameter
    protected String mimetype;

    @RequestParameter
    protected String docType;

    protected MimetypeRegistry mimetypeRegistry;

    // Event-long cache for mimetype lookups - no invalidation required
    protected final Map<String, Boolean> cachedEditableStates = new HashMap<String, Boolean>();

    // Event-long cache for document field lookups - no invalidation required
    protected final Map<String, Boolean> cachedEditableBlobs = new HashMap<String, Boolean>();

    /**
     * Creates the bootstrap file. It is called from the browser's addon. The URL composition tells the case and what to
     * create. The structure is depicted in the NXP-1881. Rux NXP-1959: add new tag on root level describing the action:
     * actionEdit, actionNew or actionFromTemplate.
     *
     * @return the bootstrap file content
     */
    public void getBootstrap() throws IOException {
        String currentRepoID = documentManager.getRepositoryName();

        CoreSession session = documentManager;
        CoreSession templateSession = documentManager;
        try {
            if (repoID != null && !currentRepoID.equals(repoID)) {
                session = CoreInstance.openCoreSession(repoID);
            }

            if (templateRepoID != null && !currentRepoID.equals(templateRepoID)) {
                templateSession = CoreInstance.openCoreSession(templateRepoID);
            }

            DocumentModel doc = null;
            DocumentModel templateDoc = null;
            String filename = null;
            if (ACTION_EDIT_DOCUMENT.equals(action)) {
                // fetch the document to edit to get its mimetype and document
                // type
                doc = session.getDocument(new IdRef(docRef));
                docType = doc.getType();
                Blob blob = null;
                if (blobPropertyName != null) {
                    blob = (Blob) doc.getPropertyValue(blobPropertyName);
                    if (blob == null) {
                        throw new NuxeoException(String.format("could not find blob to edit with property '%s'",
                                blobPropertyName));
                    }
                } else {
                    blob = (Blob) doc.getProperty(schema, blobField);
                    if (blob == null) {
                        throw new NuxeoException(String.format(
                                "could not find blob to edit with schema '%s' and field '%s'", schema, blobField));
                    }
                }
                mimetype = blob.getMimeType();
                if (filenamePropertyName != null) {
                    filename = (String) doc.getPropertyValue(filenamePropertyName);
                } else {
                    filename = (String) doc.getProperty(schema, filenameField);
                }
            } else if (ACTION_CREATE_DOCUMENT.equals(action)) {
                // creating a new document all parameters are read from the
                // request parameters
            } else if (ACTION_CREATE_DOCUMENT_FROM_TEMPLATE.equals(action)) {
                // fetch the template blob to get its mimetype
                templateDoc = templateSession.getDocument(new IdRef(templateDocRef));
                Blob blob = (Blob) templateDoc.getProperty(templateSchema, templateBlobField);
                if (blob == null) {
                    throw new NuxeoException(String.format(
                            "could not find template blob with schema '%s' and field '%s'", templateSchema,
                            templateBlobField));
                }
                mimetype = blob.getMimeType();
                // leave docType from the request query parameter
            } else {
                throw new NuxeoException(String.format(
                        "action '%s' is not a valid LiveEdit action: should be one of '%s', '%s' or '%s'", action,
                        ACTION_CREATE_DOCUMENT, ACTION_CREATE_DOCUMENT_FROM_TEMPLATE, ACTION_EDIT_DOCUMENT));
            }

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

            Element root = DocumentFactory.getInstance().createElement(liveEditTag);
            root.addNamespace("", XML_LE_NAMESPACE);
            // RUX NXP-1959: action id
            Element actionInfo = root.addElement(actionSelectorTag);
            actionInfo.setText(action);

            // Document related informations
            Element docInfo = root.addElement(documentTag);
            addTextElement(docInfo, docRefTag, docRef);
            Element docPathT = docInfo.addElement(docPathTag);
            Element docTitleT = docInfo.addElement(docTitleTag);
            if (doc != null) {
                docPathT.setText(doc.getPathAsString());
                docTitleT.setText(doc.getTitle());
            }
            addTextElement(docInfo, docRepositoryTag, repoID);

            addTextElement(docInfo, docSchemaNameTag, schema);
            addTextElement(docInfo, docFieldNameTag, blobField);
            addTextElement(docInfo, docBlobFieldNameTag, blobField);
            Element docFieldPathT = docInfo.addElement(docfieldPathTag);
            Element docBlobFieldPathT = docInfo.addElement(docBlobFieldPathTag);
            if (blobPropertyName != null) {
                // FIXME AT: NXP-2306: send blobPropertyName correctly (?)
                docFieldPathT.setText(blobPropertyName);
                docBlobFieldPathT.setText(blobPropertyName);
            } else {
                if (schema != null && blobField != null) {
                    docFieldPathT.setText(schema + ':' + blobField);
                    docBlobFieldPathT.setText(schema + ':' + blobField);
                }
            }
            addTextElement(docInfo, docFilenameFieldNameTag, filenameField);
            Element docFilenameFieldPathT = docInfo.addElement(docFilenameFieldPathTag);
            if (filenamePropertyName != null) {
                docFilenameFieldPathT.setText(filenamePropertyName);
            } else {
                if (schema != null && blobField != null) {
                    docFilenameFieldPathT.setText(schema + ':' + filenameField);
                }
            }

            addTextElement(docInfo, docfileNameTag, filename);
            addTextElement(docInfo, docTypeTag, docType);
            addTextElement(docInfo, docMimetypeTag, mimetype);
            addTextElement(docInfo, docFileExtensionTag, getFileExtension(mimetype));

            Element docFileAuthorizedExtensions = docInfo.addElement(docFileAuthorizedExtensionsTag);
            List<String> authorizedExtensions = getFileExtensions(mimetype);
            if (authorizedExtensions != null) {
                for (String extension : authorizedExtensions) {
                    addTextElement(docFileAuthorizedExtensions, docFileAuthorizedExtensionTag, extension);
                }
            }

            Element docIsVersionT = docInfo.addElement(docIsVersionTag);
            Element docIsLockedT = docInfo.addElement(docIsLockedTag);
            if (ACTION_EDIT_DOCUMENT.equals(action)) {
                docIsVersionT.setText(Boolean.toString(doc.isVersion()));
                docIsLockedT.setText(Boolean.toString(doc.isLocked()));
            }

            // template information for ACTION_CREATE_DOCUMENT_FROM_TEMPLATE

            Element templateDocInfo = root.addElement(templateDocumentTag);
            addTextElement(templateDocInfo, docRefTag, templateDocRef);
            docPathT = templateDocInfo.addElement(docPathTag);
            docTitleT = templateDocInfo.addElement(docTitleTag);
            if (templateDoc != null) {
                docPathT.setText(templateDoc.getPathAsString());
                docTitleT.setText(templateDoc.getTitle());
            }
            addTextElement(templateDocInfo, docRepositoryTag, templateRepoID);
            addTextElement(templateDocInfo, docSchemaNameTag, templateSchema);
            addTextElement(templateDocInfo, docFieldNameTag, templateBlobField);
            addTextElement(templateDocInfo, docBlobFieldNameTag, templateBlobField);
            docFieldPathT = templateDocInfo.addElement(docfieldPathTag);
            docBlobFieldPathT = templateDocInfo.addElement(docBlobFieldPathTag);
            if (templateSchema != null && templateBlobField != null) {
                docFieldPathT.setText(templateSchema + ":" + templateBlobField);
                docBlobFieldPathT.setText(templateSchema + ":" + templateBlobField);
            }
            addTextElement(templateDocInfo, docMimetypeTag, mimetype);
            addTextElement(templateDocInfo, docFileExtensionTag, getFileExtension(mimetype));

            Element templateFileAuthorizedExtensions = templateDocInfo.addElement(docFileAuthorizedExtensionsTag);
            if (authorizedExtensions != null) {
                for (String extension : authorizedExtensions) {
                    addTextElement(templateFileAuthorizedExtensions, docFileAuthorizedExtensionTag, extension);
                }
            }

            // Browser request related informations
            Element requestInfo = root.addElement(requestInfoTag);
            Cookie[] cookies = request.getCookies();
            Element cookiesT = requestInfo.addElement(requestCookiesTag);
            for (Cookie cookie : cookies) {
                Element cookieT = cookiesT.addElement(requestCookieTag);
                cookieT.addAttribute("name", cookie.getName());
                cookieT.setText(cookie.getValue());
            }
            Element headersT = requestInfo.addElement(requestHeadersTag);
            Enumeration hEnum = request.getHeaderNames();
            while (hEnum.hasMoreElements()) {
                String hName = (String) hEnum.nextElement();
                if (!hName.equalsIgnoreCase("cookie")) {
                    Element headerT = headersT.addElement(requestHeaderTag);
                    headerT.addAttribute("name", hName);
                    headerT.setText(request.getHeader(hName));
                }
            }
            addTextElement(requestInfo, requestBaseURLTag, BaseURL.getBaseURL(request));

            // User related informations
            String username = context.getExternalContext().getUserPrincipal().getName();
            Element userInfo = root.addElement(userInfoTag);
            addTextElement(userInfo, userNameTag, username);
            addTextElement(userInfo, userPasswordTag, "");
            addTextElement(userInfo, userTokenTag, "");
            addTextElement(userInfo, userLocaleTag, context.getViewRoot().getLocale().toString());
            // Rux NXP-1882: the wsdl locations
            String baseUrl = BaseURL.getBaseURL(request);
            Element wsdlLocations = root.addElement(wsdlLocationsTag);
            Element wsdlAccessWST = wsdlLocations.addElement(wsdlAccessWebServiceTag);
            wsdlAccessWST.setText(baseUrl + "webservices/nuxeoAccess?wsdl");
            Element wsdlEEWST = wsdlLocations.addElement(wsdlLEWebServiceTag);
            wsdlEEWST.setText(baseUrl + "webservices/nuxeoLEWS?wsdl");

            // Server related informations
            Element serverInfo = root.addElement(serverInfoTag);
            Element serverVersionT = serverInfo.addElement(serverVersionTag);
            serverVersionT.setText("5.1"); // TODO: use a buildtime generated
            // version tag instead

            // Client related informations
            Element editId = root.addElement(editIdTag);
            editId.setText(getEditId(doc, session, username));

            // serialize bootstrap XML document in the response
            Document xmlDoc = DocumentFactory.getInstance().createDocument();
            xmlDoc.setRootElement(root);
            response.setContentType("text/xml; charset=UTF-8");

            // use a formatter to make it easier to debug live edit client
            // implementations
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            XMLWriter writer = new XMLWriter(response.getOutputStream(), format);
            writer.write(xmlDoc);

            response.flushBuffer();
            context.responseComplete();
        } finally {
            if (session != null && session != documentManager) {
                ((CloseableCoreSession) session).close();
            }
            if (templateSession != null && templateSession != documentManager) {
                ((CloseableCoreSession) templateSession).close();
            }
        }
    }

    protected String getFileExtension(String mimetype) {
        if (mimetype == null) {
            return null;
        }
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        if (extensions != null && !extensions.isEmpty()) {
            return extensions.get(0);
        } else {
            return null;
        }
    }

    protected List<String> getFileExtensions(String mimetype) {
        if (mimetype == null) {
            return null;
        }
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        return extensions;
    }

    protected static Element addTextElement(Element parent, QName newElementName, String value) {
        Element element = parent.addElement(newElementName);
        if (value != null) {
            element.setText(value);
        }
        return element;
    }

    // TODO: please explain what is the use of the "editId" tag here
    protected static String getEditId(DocumentModel doc, CoreSession session, String userName) {
        StringBuilder sb = new StringBuilder();

        if (doc != null) {
            sb.append(doc.getId());
        } else {
            sb.append("NewDocument");
        }
        sb.append('-');
        sb.append(session.getRepositoryName());
        sb.append('-');
        sb.append(userName);
        Calendar modified = null;
        if (doc != null) {
            try {
                modified = (Calendar) doc.getProperty(DUBLINCORE_SCHEMA, MODIFIED_FIELD);
            } catch (PropertyException e) {
                modified = null;
            }
        }
        if (modified == null) {
            modified = Calendar.getInstance();
        }
        sb.append('-');
        sb.append(modified.getTimeInMillis());
        return sb.toString();
    }

    //
    // Methods to check whether or not to display live edit links
    //

    /**
     * @deprecated use {@link #isLiveEditable(DocumentModel doc, String blobXpath)}
     */
    @Deprecated
    public boolean isLiveEditable(Blob blob) {
        if (blob == null) {
            return false;
        }
        String mimetype = blob.getMimeType();
        return isMimeTypeLiveEditable(mimetype);
    }

    /**
     * @param document the document to edit.
     * @param blobXPath XPath to the blob property
     * @return true if the document is immutable and the blob's mime type is supported, false otherwise.
     * @since 5.4
     */
    public boolean isLiveEditable(DocumentModel document, Blob blob) {
        if (document.isImmutable()) {
            return false;
        }
        // NXP-14476: Testing lifecycle state is part of the "mutable_document" filter
        if (document.getCurrentLifeCycleState().equals(LifeCycleConstants.DELETED_STATE)) {
            return false;
        }
        if (blob == null) {
            return false;
        }
        String mimetype = blob.getMimeType();
        return isMimeTypeLiveEditable(mimetype);
    }

    public boolean isMimeTypeLiveEditable(Blob blob) {
        if (blob == null) {
            return false;
        }
        String mimetype = blob.getMimeType();
        return isMimeTypeLiveEditable(mimetype);
    }

    public boolean isMimeTypeLiveEditable(String mimetype) {

        Boolean isEditable = cachedEditableStates.get(mimetype);
        if (isEditable == null) {

            if (liveEditClientConfig.getLiveEditConfigurationPolicy().equals(LiveEditClientConfig.LE_CONFIG_CLIENTSIDE)) {
                // only trust client config
                isEditable = liveEditClientConfig.isMimeTypeLiveEditable(mimetype);
                cachedEditableStates.put(mimetype, isEditable);
                return isEditable;
            }

            MimetypeEntry mimetypeEntry = getMimetypeRegistry().getMimetypeEntryByMimeType(mimetype);
            if (mimetypeEntry == null) {
                isEditable = Boolean.FALSE;
            } else {
                isEditable = mimetypeEntry.isOnlineEditable();
            }

            if (liveEditClientConfig.getLiveEditConfigurationPolicy().equals(LiveEditClientConfig.LE_CONFIG_BOTHSIDES)) {
                boolean isEditableOnClient = liveEditClientConfig.isMimeTypeLiveEditable(mimetype);
                isEditable = isEditable && isEditableOnClient;
            }
            cachedEditableStates.put(mimetype, isEditable);
        }
        return isEditable;
    }

    @Factory(value = "msword_liveeditable", scope = ScopeType.SESSION)
    public boolean isMSWordLiveEdititable() {
        return isMimeTypeLiveEditable("application/msword");
    }

    @Factory(value = "msexcel_liveeditable", scope = ScopeType.SESSION)
    public boolean isMSExcelLiveEdititable() {
        return isMimeTypeLiveEditable("application/vnd.ms-excel");
    }

    @Factory(value = "mspowerpoint_liveeditable", scope = ScopeType.SESSION)
    public boolean isMSPowerpointLiveEdititable() {
        return isMimeTypeLiveEditable("application/vnd.ms-powerpoint");
    }

    @Factory(value = "ootext_liveeditable", scope = ScopeType.SESSION)
    public boolean isOOTextLiveEdititable() {
        return isMimeTypeLiveEditable("application/vnd.oasis.opendocument.text");
    }

    @Factory(value = "oocalc_liveeditable", scope = ScopeType.SESSION)
    public boolean isOOCalcLiveEdititable() {
        return isMimeTypeLiveEditable("application/vnd.oasis.opendocument.spreadsheet");
    }

    @Factory(value = "oopresentation_liveeditable", scope = ScopeType.SESSION)
    public boolean isOOPresentationLiveEdititable() {
        return isMimeTypeLiveEditable("application/vnd.oasis.opendocument.presentation");
    }

    public boolean isCurrentDocumentLiveEditable() {
        return isDocumentLiveEditable(navigationContext.getCurrentDocument(), DEFAULT_SCHEMA, DEFAULT_BLOB_FIELD);
    }

    public boolean isCurrentDocumentLiveEditable(String schemaName, String fieldName) {
        return isDocumentLiveEditable(navigationContext.getCurrentDocument(), schemaName, fieldName);
    }

    public boolean isCurrentDocumentLiveEditable(String propertyName) {
        return isDocumentLiveEditable(navigationContext.getCurrentDocument(), propertyName);
    }

    public boolean isDocumentLiveEditable(DocumentModel documentModel, String schemaName, String fieldName)
            {
        return isDocumentLiveEditable(documentModel, schemaName + ":" + fieldName);
    }

    public boolean isDocumentLiveEditable(DocumentModel documentModel, String propertyName) {
        if (documentModel == null) {
            log.warn("cannot check live editable state of null DocumentModel");
            return false;
        }

        // NXP-14476: Testing lifecycle state is part of the "mutable_document" filter
        if (LifeCycleConstants.DELETED_STATE.equals(documentModel.getCurrentLifeCycleState())) {
            return false;
        }

        // check Client browser config
        if (!liveEditClientConfig.isLiveEditInstalled()) {
            return false;
        }

        String cacheKey = documentModel.getRef() + "__" + propertyName;
        Boolean cachedEditableBlob = cachedEditableBlobs.get(cacheKey);
        if (cachedEditableBlob == null) {

            if (documentModel.hasFacet(FacetNames.IMMUTABLE)) {
                return cacheBlobToFalse(cacheKey);
            }

            if (!documentManager.hasPermission(documentModel.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
                // the lock state is check as a extension to the
                // SecurityPolicyManager
                return cacheBlobToFalse(cacheKey);
            }

            Blob blob;
            try {
                blob = documentModel.getProperty(propertyName).getValue(Blob.class);
            } catch (PropertyException e) {
                // this document cannot host a live editable blob is the
                // requested property, ignore
                return cacheBlobToFalse(cacheKey);
            }
            cachedEditableBlob = isLiveEditable(blob);
            cachedEditableBlobs.put(cacheKey, cachedEditableBlob);
        }
        return cachedEditableBlob;
    }

    protected boolean cacheBlobToFalse(String cacheKey) {
        cachedEditableBlobs.put(cacheKey, Boolean.FALSE);
        return false;
    }

    protected MimetypeRegistry getMimetypeRegistry() {
        if (mimetypeRegistry == null) {
            mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        }
        return mimetypeRegistry;
    }

}
