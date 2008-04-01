/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.filemanager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.util.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.api.ws.DocumentDescriptor;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioning;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioningBean;
import org.nuxeo.runtime.api.Framework;

/**
 * Web service providing functions for loading a document into an external
 * editor (like word) using LiveEdit, locking and saving with different options
 * (versioning incrementation, etc). <p/> The LiveEdit client performs calls as
 * described in the next sequence:
 * <ul>
 * <li>edit : in fact this is called by a browser link to create a liveedit
 * specific file </li>
 * <li>doPreEditActions</li>
 * <li>getDataForExternalEdit</li>
 * <li>editWithActions : for updating the document</li>
 * </ul>
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Stateless
@Name("FileManageWS")
@Scope(ScopeType.CONVERSATION)
@SerializedConcurrentAccess
@Remote(FileManageActionsRemote.class)
@Local(FileManageWSLocal.class)
@WebService(name = "FileManageInterface", serviceName = "FileManageService")
@SOAPBinding(style = Style.DOCUMENT)
public class FileManageWS extends InputController implements
        FileManageActionsRemote, FileManageWSLocal {

    private static final Log log = LogFactory.getLog(FileManageWS.class);

    private static final String CURRENT_EDITED_DOCUMENT = "liveEditEditedDocument";
    private static final String CURRENT_EDITED_REPOSITORY_LOCATION = "liveEditEditedRepositoryLocation";


    private static final String DOC_URL = "doc_url";

    private static final String DOC_UUID = "doc_uuid";

    private static final String FIELD_NAME = "field_name";

    private static final String FILE_NAME = "file_name";

    private static final String META_TYPE = "meta_type";

    private static final String CONTENT_TYPE = "content_type";

    private static final String SERVER_VERSION = "ServerVersion";

    private static final String WS_ADRESS = "wsaddress";

    private static final String JSESSION_ID = "JSESSIONID";

    private static final String NEW_LINE = "\n";

    private static final String COOKIE = "Cookie";

    // XXX need to use accessors from navigationContext instead
    // @In(required = false)
    // @Out(required = false)
    // protected DocumentModel changeableDocument;

    @In(create = true, required=false)
    protected CoreSession documentManager;

    @In(required = false, create = true)
    protected transient TypesTool typesTool;

    @In(create = true, required=false)
    protected DocumentVersioning documentVersioning;

    @RequestParameter
    String fileFieldFullName;

    @RequestParameter
    String filenameFieldFullName;

    @RequestParameter
    String filename;

    @RequestParameter
    String docRef;

    @Remove
    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    public String display() {
        return "view_documents";
    }

    // @WebServiceRef(name = "FileManageInterface")
    // FileManageWSLocal service;
    // @WebServiceRef(name = "FileManageWS")
    // FileManageWSLocal service2;

    /**
     * This method is called from a page action link to generate a file used by
     * client IE plugin
     * @deprecated use nxd:liveEditUrl function defined in DocumentModelFunctions instead
     */
    @Deprecated
    public String edit() throws ClientException {

        final String logPrefix = "<edit> ";

        DocumentModel currentDocument =navigationContext.getCurrentDocument();
        // store for later usage
        setEditedDocument(currentDocument);
        setEditedRepositoryName(navigationContext.getCurrentServerLocation());

        try {
            if (fileFieldFullName == null) {
                return null;
            }

            String[] s = fileFieldFullName.split(":");
            Blob blob = (Blob) currentDocument.getProperty(
                    s[0], s[1]);

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String requestUri = request.getRequestURI();
            StringBuffer requestUrl = request.getRequestURL();
            String startPath = requestUrl.substring(0,
                    requestUrl.indexOf(requestUri));
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"nx5_edit.nuxeo5\";");
            /* String docUrl = startPath + currentDocument.getPathAsString(); */
            String docUrl = currentDocument.getPathAsString();
            response.getWriter().write(DOC_URL + " : " + docUrl + NEW_LINE);
            response.getWriter().write(
                    DOC_UUID + " : "
                            + currentDocument.getId()
                            + NEW_LINE);
            response.getWriter().write(
                    FIELD_NAME + " : " + fileFieldFullName + NEW_LINE);

            String fileName = null;
            if (filename != null && !filename.equals("")) {
                fileName = filename;
            } else {
                // try to fetch it from given field
                if (filenameFieldFullName != null) {
                    s = filenameFieldFullName.split(":");
                    fileName = (String) currentDocument.getProperty(
                            s[0], s[1]);
                }
            }
            if (fileName == null || fileName.equals("")) {
                fileName = "file";
            }
            response.getWriter().write(FILE_NAME + " : " + fileName + NEW_LINE);
            String metaType = "Resource";
            response.getWriter().write(META_TYPE + " : " + metaType + NEW_LINE);
            String contentType = blob.getMimeType();
            // TODO : Remove this part when bug on switch in mimetype and
            // encoding has been solved
            if (contentType == null && blob.getEncoding() != null) {
                contentType = blob.getEncoding();
            }

            log.info(logPrefix + CONTENT_TYPE + ':' + contentType);
            response.getWriter().write(
                    CONTENT_TYPE + " : " + contentType + NEW_LINE);
            String serverVersion = "Nuxeo5";
            response.getWriter().write(
                    SERVER_VERSION + " : " + serverVersion + NEW_LINE);
            // FIXME : We can probably also retrieve the '/nuxeo/' in the path

            // String servicePath = new JBossServicesProxy().getURI();
            // String wsaddress = startPath + "" + servicePath;

            String wsaddress = startPath + "/nuxeo/ws/"
                    + getClass().getSimpleName();
            response.getWriter().write(WS_ADRESS + " : " + wsaddress + NEW_LINE);

            Cookie[] cookies = request.getCookies();
            List<String> cookieItems = new LinkedList<String>();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(JSESSION_ID)) {
                    // special handling of the JSESSIONID cookie
                    response.getWriter().write(
                            JSESSION_ID + " : " + cookie.getValue() + NEW_LINE);
                } else {
                    // forward all other cookies in a single line
                    cookieItems.add(cookie.getName() + '=' + cookie.getValue());
                }
            }
            String resultCookies = COOKIE + " : "
                    + StringUtils.join(cookieItems.iterator(), ";");

            response.getWriter().write(resultCookies + NEW_LINE);

            response.getWriter().write(NEW_LINE);
            log.info("Editing with: " + contentType + ';');
            response.setContentType("application/nx5-edit");
            response.getWriter().flush();
            blob.transferTo(response.getOutputStream());
            response.flushBuffer();
            context.responseComplete();
            return null;
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public void initialize() {
        log.info("Initializing...");
    }

    public DocumentModel _getChangeableDocument() {
        // return navigationContext.getChangeableDocument();
        return navigationContext.getCurrentDocument();
    }

    public void _setChangeableDocument(DocumentModel documentModel) {
        navigationContext.setChangeableDocument(documentModel);
        // navigationContext.setCurrentDocument(documentModel);
    }

    /**
     * The client must call this method to do the actions the user selected. The
     * return parameter will be the url of the document. This url can be the
     * same as the orginal one, or a new one.
     *
     * @param doc_url
     * @param actionID
     * @return
     * @throws ClientException
     */
    @WebMethod
    public String doPreEditActions(String doc_url, String actionID)
            throws ClientException {
        log.debug("<doPreEditActions> : " + actionID);
        if (actionID.equals("Lock")) {
            log.debug("Need to lock Document");
            return lockDocument();
        }
        return null;
    }

    private Hashtable<String, String> getHashTable(String[] updateFields) {
        Hashtable<String, String> fields = new Hashtable<String, String>();
        for (int i = 0; i < (updateFields.length / 2); i++) {
            fields.put(updateFields[2 * i], updateFields[2 * i + 1]);
        }
        return fields;
    }

    /**
     * Updates the document by setting received field values and performing the
     * given action that could specify version incrementation and/or unlocking
     * the document.
     * <p>
     * This method could be called by any SOAP client, so we must check for the
     * parameters.
     */
    // TODO this method has a bad name: replace with something like
    // updateDocument...
    @WebMethod
    public String editWithActions(String[] updateFields, String actionID)
            throws ClientException {

        final String logPrefix = "<editWithActions> ";
        if (log.isDebugEnabled()) {
            String debugInfo = "";
            for (String key : updateFields) {
                debugInfo += key + "\n ------- \n ";
            }
            log.debug(logPrefix + "actionID : " + actionID + "/ \n "
                    + debugInfo);
        }

        // TODO : is it supposed to be received as request param?
        // if not, remove it as a class field
        // if (fileFieldFullName == null)
        // return null;

        Hashtable<String, String> fields = getHashTable(updateFields);

        fileFieldFullName = fields.get(FIELD_NAME);

        log.debug(logPrefix + "fileFieldFullName = " + fileFieldFullName);

        if (null == fileFieldFullName) {
            throw new ClientException(logPrefix + "param '" + FIELD_NAME
                    + "' missing.");
        }

        String docUrl = fields.get(DOC_URL);
        if (null == docUrl) {
            throw new ClientException(logPrefix + "param '" + DOC_URL
                    + "' missing.");
        }
        PathRef docRef = new PathRef(docUrl);
        // setChangeableDocument(documentManager.getDocument(docRef));
        final DocumentModel docModel = getCurrentDocument();
        // currentDocument;documentManager.getDocument(docRef);

        // Updating Content
        String content = fields.get(fileFieldFullName);
        // log.info("content ====== \n" + content + " \n ==============");
        // TODO create a Base64 wrapper in NXCommon to avoid additional third -
        // party
        // lib deps
        final byte[] contentData = Base64.decode(content);
        final String[] prop = fileFieldFullName.split(":");
        String contentType = fields.get(CONTENT_TYPE);
        if (contentType == null) {
            // not sent, reloading
            final Blob blob = (Blob) docModel.getProperty(prop[0], prop[1]);
            if (null == blob) {
                // maybe it was erased or other error occured
                throw new ClientException(
                        "cannot retrieve file content for document to be saved");
            }
            contentType = blob.getMimeType();
        }
        StreamingBlob input = StreamingBlob.createFromByteArray(contentData,
                contentType);
        // DocumentModel changeableDocument = getChangeableDocument();
        // changeableDocument.setProperty(prop[0], prop[1], input);
        // set document file content
        docModel.setProperty(prop[0], prop[1], input);

        // Updating Name
        String fileName = fields.get(FILE_NAME);
        if (filenameFieldFullName != null) {
            String[] s = filenameFieldFullName.split(":");
            // changeableDocument.setProperty(s[0], s[1], fileName);
            docModel.setProperty(s[0], s[1], fileName);
        }
        unlockDocument();

        // performs Action depending on action_id
        // if (actionID.equals("Unlock")) {
        // log.debug("Need to unlock Document");
        // return unlockDocument();
        // }
        if (actionID.equals("Save")) {
            log.debug("Saving Document");
            return updateDocument(docModel);
        }
        if (actionID.equals("SaveNew")) {
            log.debug("Saving Document as New version");
            // return updateDocumentAsNewVersion(docModel);
            docModel.putContextData(
                    org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
            return updateDocument(docModel);
        }

        // check versioning actions
        final VersioningActions selectedOption = VersioningActions.valueOf(actionID);
        if (selectedOption != null) {
            log.info("Saving Document with versioning option: "
                    + selectedOption);

            // documentVersioning.incrementVersions(changeableDocument,
            // selectedOption);
            DocumentVersioningBean.setVersioningOptionInstanceId(docModel,
                    selectedOption, true);

            return updateDocument(docModel);
        }

        return null;
    }

    // XXX AT: i dont get why documentActions is not enough
    // DM: maybe because documentActions raises events through context
    // inexistent in case of ws call
    /**
     * @param docModel
     * @return
     * @throws ClientException
     * @deprecated use UploadFileRestlet instead
     */
    @Deprecated
    public String updateDocument(DocumentModel docModel) throws ClientException {
        try {
            // DocumentModel changeableDocument = getChangeableDocument();

            docModel = getDocumentManager().saveDocument(docModel);
            getDocumentManager().save();

            if (getCurrentDocument().getRef().equals(
                    docModel.getRef())) {
                // contextManager.updateContext(changeableDocument);
                // navigationContext.updateDocumentContext(changeableDocument);
                // navigationContext.invalidateCurrentDocument();
            }
            // TODO temporarily disabled to avoid problems with
            // comment observer getting FacesContext, etc
            // eventManager.raiseEventsOnDocumentChange(changeableDocument);
            // return navigationContext.navigateToDocument(changeableDocument,
            // "after-edit");
            return ""; // non-null
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    // XXX AT: i dont get why documentActions is not enough
    /**
     * @deprecated we should save a new version through doc context flags, not
     *             directly
     */
    @Deprecated
    public String updateDocumentAsNewVersion(DocumentModel docModel)
            throws ClientException {
        try {
            // save the changed data to the current working version
            String result = updateDocument(docModel);

            // Do a checkin / checkout of the edited version
            DocumentRef docRef = docModel.getRef();
            VersionModel newVersion = new VersionModelImpl();
            newVersion.setLabel(getDocumentManager().generateVersionLabelFor(docRef));
            getDocumentManager().checkIn(docRef, newVersion);
            logDocumentWithTitle("Checked in ", docModel);
            getDocumentManager().checkOut(docRef);
            logDocumentWithTitle("Checked out ", docModel);

            // then follow the standard pageflow for edited documents
            return result;

        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Used by the client to fetch the new document, if the doPreEditActions
     * returned a different URL. NB : if the url has not changed, the plugin
     * already has the needed data.
     *
     * @param doc_url
     * @param fieldName
     * @return
     * @throws ClientException
     * @deprecated use LiveEditHelper + nxd:liveEditUrl() JSF function instead
     */
    @Deprecated
    @WebMethod
    public String getDataForExternalEdit(@WebParam(name = "doc_url")
    String doc_url, @WebParam(name = "fieldName")
    String fieldName) throws ClientException {
        try {
            log.debug("getDataForExternalEdit : " + fieldName + '/' + doc_url);
            PathRef docRef = new PathRef(doc_url);
            DocumentModel docModel = getDocumentManager().getDocument(docRef);
            String[] prop = fieldName.split(":");
            Blob content = (Blob) docModel.getProperty(prop[0], prop[1]);
            byte[] output = content.getByteArray();
            return new String(output);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * The client must call this method on the doc before saving.
     * <p>
     * This methods returns a HashMap listing all possibles actions with their
     * desciption. Ex: { 'lock' : "Locking the document", 'lock+version' : "Lock
     * and version the document" ...} The returned actions are all exclusives,
     * and the user will have to chose one of the options.
     *
     * @param doc_url
     * @return
     * @throws ClientException
     */
    @WebMethod
    public String[] getPostEditActions(String doc_url) throws ClientException {

        final String logPrefix = "<getPostEditActions> ";

        final List<String> postEditActList = new ArrayList<String>();

        final PathRef docRef = new PathRef(doc_url);
        // setChangeableDocument(documentManager.getDocument(docRef));
        // DocumentModel changeableDocument = getChangeableDocument();
        DocumentModel docModel = getDocumentManager().getDocument(docRef);

        // XXX: DM this check is not needed
        if (docModel != null) {
            final Map<String, String> versioningOptionsMap = documentVersioning.getVersioningOptionsMap(
                    docModel);

            // add versioning options
            for (final String optionKey : versioningOptionsMap.keySet()) {
                final String optionValue = versioningOptionsMap.get(optionKey);
                postEditActList.add(optionKey);
                postEditActList.add(optionValue);
            }
        } else {
            log.warn(logPrefix + "changeableDocument is null");
        }
        /*
         * postEditActList.add("Unlock");
         * postEditActList.add(getTranslation("Unlock"));
         * postEditActList.add("Save"); postEditActList.add("Save_Document");
         * postEditActList.add("SaveNew");
         * postEditActList.add("Save_in_new_Version");
         */

        log.info(logPrefix + "postEditActList: " + postEditActList);

        String[] postEditAct = new String[postEditActList.size()];
        postEditAct = postEditActList.toArray(postEditAct);
        return postEditAct;
    }

    /**
     * The client must call this method on the doc when the edit process starts.
     * <p>
     * This methods returns a HashMap listing all possibles actions with their
     * desciption. Ex: { 'lock' : "Locking the document", 'lock+version' : "Lock
     * and version the document" ...}
     * <p>
     * The returned actions are all exclusives, and the user will have to chose
     * one of the options.
     *
     * @param doc_url
     * @return
     * @throws ClientException
     */
    @WebMethod
    public String[] getPreEditActions(String doc_url) throws ClientException {
        List<String> preEditActList = new ArrayList<String>();
        preEditActList.add(getTranslation("Lock"));
        preEditActList.add("Lock");
        return preEditActList.toArray(new String[0]);
    }

    public Map<String, Object> createDataMap(String[] propertiesArray) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (int i = 0; i < propertiesArray.length; i += 2) {
            String key = propertiesArray[i];
            String value = propertiesArray[i + 1];
            String[] path = key.split("\\.");

            createSubMaps(map, path, value, 0);
        }

        return map;
    }

    private void createSubMaps(Map<String, Object> map, String[] path,
            String value, int depth) {
        String key = path[depth];

        if (depth == path.length - 1) {
            map.put(key, value);
        } else {
            Map<String, Object> subMap = (Map<String, Object>) map.get(key);
            if (subMap == null) {
                subMap = new HashMap<String, Object>();
                map.put(path[depth], subMap);
            }
            createSubMaps(subMap, path, value, depth + 1);
        }
    }

    /**
     * @deprecated use CreateDocumentRestlet / UploadFileRestlet instead
     */
    @Deprecated
    @WebMethod
    public String createDocument(String parentUUID, String type,
            String[] properties) throws ClientException {
        // TODO Note: This method is intented to be a general method, but now it
        // can only be used by LiveEdit
        // In the future, a new method (which will set the properties of a
        // document from a given map) will be probably
        // available in org.nuxeo.ecm.core.api.impl.DocumentHelper and then this
        // method will be made "general".

        String name = "file_" + System.currentTimeMillis();
        DocumentRef parentRef = new IdRef(parentUUID);
        DocumentModel document = new DocumentModelImpl(
                getDocumentManager().getDocument(parentRef), name, type);

        document = getDocumentManager().createDocument(document);

        Map<String, Object> propertiesMap = createDataMap(properties);

        Map<String, Object> fileMap = (Map<String, Object>) propertiesMap.get("file");
        Map<String, Object> contentMap = (Map<String, Object>) fileMap.get("content");
        Map<String, Object> dublincoreMap = (Map<String, Object>) propertiesMap.get("dublincore");

        document.setProperty("dublincore", "description",
                dublincoreMap.get("description"));
        document.setProperty("dublincore", "title", dublincoreMap.get("title"));

        String filename = (String) fileMap.get("filename");
        document.setProperty("file", "filename", filename);
        final byte[] contentData = Base64.decode((String) contentMap.get("data"));

        Blob blob = StreamingBlob.createFromByteArray(contentData);
        try {
            MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
            blob.setMimeType(mimeService.getMimetypeFromFilenameAndBlobWithDefault(
                    filename, blob, blob.getMimeType()));
        } catch (MimetypeDetectionException e) {
            log.error(String.format(
                    "error during mimetype detection for %s: %s", filename,
                    e.getMessage()));
        } catch (Exception e) {
            log.error(String.format(
                    "error during mimetype service access for %s: %s",
                    filename, e.getMessage()));
        }

        blob.setEncoding((String) contentMap.get("encoding"));
        document.setProperty("file", "content", blob);

        getDocumentManager().saveDocument(document);
        getDocumentManager().save();

        return null;
    }

    @WebMethod
    public ACE[] getDocumentACLFMWS(String uuid) throws ClientException {
        ACP acp = getDocumentManager().getACP(new IdRef(uuid));
        if (acp != null) {
            ACL acl = acp.getMergedACLs("MergedACL");
            ACE[] aces = acl.toArray(new ACE[acl.size()]);
            String currentUsername = getDocumentManager().getPrincipal().getName();
            List<ACE> returnAces = new ArrayList<ACE>();
            for (ACE ace : aces) {
                if (ace.getUsername().equals(currentUsername)) {
                    returnAces.add(ace);
                }
            }
            return returnAces.toArray(new ACE[0]);
        } else {
            return null;
        }
    }

    @WebMethod
    public boolean canCreateChildren(String parentUUID, String documentType)
            throws ClientException {
        boolean returnFlag = false;

        ACP acp = getDocumentManager().getACP(new IdRef(parentUUID));
        if (acp != null) {
            ACL acl = acp.getMergedACLs("MergedACL");
            ACE[] aces = acl.toArray(new ACE[acl.size()]);
            String currentUsername = getDocumentManager().getPrincipal().getName();
            for (ACE ace : aces) {
                if (ace.getUsername().equals(currentUsername)
                        && ace.getPermission().equals(
                                SecurityConstants.ADD_CHILDREN)) {
                    returnFlag = true;
                    break;
                }
            }
        }

        String parentDocumentType = getDocumentManager().getDocument(
                new IdRef(parentUUID)).getType();
        List<String> allowedSubTypes = typesTool.getAllowedSubTypesFor(
                parentDocumentType);
        for (String type : allowedSubTypes) {
            if (type.equals(documentType)) {
                returnFlag = true;
                break;
            }
        }

        return returnFlag;
    }

    @WebMethod
    public DocumentDescriptor getRootDocumentFMWS() throws ClientException {
        DocumentModel doc = getDocumentManager().getRootDocument();
        return doc != null ? new DocumentDescriptor(doc) : null;
    }

    @WebMethod
    public DocumentDescriptor[] getChildrenFMWS(String uuid)
            throws ClientException {
        DocumentModelList docList = getDocumentManager().getChildren(
                new IdRef(uuid));
        DocumentDescriptor[] docs = new DocumentDescriptor[docList.size()];
        int i = 0;
        for (DocumentModel doc : docList) {
            docs[i++] = new DocumentDescriptor(doc);
        }
        return docs;
    }

    private String getTranslation(String key) {
        // TODO need to externalize this with an ENUM... and make keys
        // public
        final String fullKey = "command.option.liveedit.preeditaction."
                + key.toLowerCase();

        return resourcesAccessor.getMessages().get(fullKey);
    }

    public String unlockDocument() throws ClientException {
        if (getDocumentManager().getLock(
                getCurrentDocument().getRef()) != null) {
            getDocumentManager().unlock(
                    getCurrentDocument().getRef());
            getDocumentManager().save();
        }
        return null;
    }

    public String lockDocument() throws ClientException {
        if (getDocumentManager().getLock(
                getCurrentDocument().getRef()) == null) {
            getDocumentManager().setLock(
                    getCurrentDocument().getRef(),
                    getDocumentLockKey());
            getDocumentManager().save();
        }
        return null;
    }

    private String getDocumentLockKey() throws ClientException {
        StringBuilder result = new StringBuilder();
        result.append(getDocumentManager().getPrincipal().getName()).append(':').append(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));
        return result.toString();
    }

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    public String editSelectedDocument() throws ClientException {
        if (docRef != null) {
            try {
                navigationContext.setCurrentDocument(
                        getDocumentManager().getDocument(new IdRef(docRef)));
            } catch (Exception e) {
                throw new ClientException(
                        "Erreur lors de la recuperation du document ? partir de l'ID "
                                + docRef);
            }
        }
        return edit();
    }

    // Compatibility method to make the Component work
    // even when conversation context is not properly restored
    // => this is the case for WS access
    // ==> use Session context to store currentDoc and currentRepo

    private void setEditedRepositoryName(RepositoryLocation repoLoc) {
        final Context sessionContext = Contexts.getSessionContext();
        sessionContext.set(CURRENT_EDITED_REPOSITORY_LOCATION, repoLoc);
    }

    private void setEditedDocument(DocumentModel doc) {
        final Context sessionContext = Contexts.getSessionContext();
        sessionContext.set(CURRENT_EDITED_DOCUMENT, doc);
    }

    private CoreSession getDocumentManager() throws ClientException {
        if (documentManager == null) {
            documentManager = (CoreSession) Component.getInstance(
                    "documentManager", true);
        }
        if (documentManager == null) {
            DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts(
                    "documentManager");
            if (documentManagerBD == null) {
                documentManagerBD = new DocumentManagerBusinessDelegate();
            }
            RepositoryLocation serverLoc = getEditedRepositoryLocation();
            documentManager = documentManagerBD.getDocumentManager(serverLoc);
            Contexts.getConversationContext().set("currentServerLocation",
                    serverLoc);
        }
        return documentManager;
    }

    private DocumentModel getCurrentDocument() {
        DocumentModel currentDocument = null;
        if (navigationContext != null) {
            currentDocument = navigationContext.getCurrentDocument();
        }

        if (currentDocument == null) {
            final Context sessionContext = Contexts.getSessionContext();
            currentDocument = (DocumentModel) sessionContext.get(
                    CURRENT_EDITED_DOCUMENT);
        }
        return currentDocument;
    }

    private RepositoryLocation getEditedRepositoryLocation() {
        final Context sessionContext = Contexts.getSessionContext();
        return (RepositoryLocation) sessionContext.get(
                CURRENT_EDITED_REPOSITORY_LOCATION);
    }

}
