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
 * $Id: LiveEditBootstrapHelper.java 30586 2008-02-26 14:30:17Z ogrisel $
 */

package org.nuxeo.ecm.webapp.liveedit;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Enumeration;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * The LiveEdit bootstrap procedure works as follows:
 * <ul>
 *
 * <li>browsed page calls a JSF function from the DocumentModelFunctions class
 * (edit a document, create new document, etc.) to generate;</li>
 *
 * <li>composing a specifc URL as result, triggering the bootstrap addon to
 * popup;</li>
 *
 * <li>the addon come back with the URL composed allowing the present seam
 * component to create the bootstrap file. The file contains various data as
 * requested in the URL;</li>
 *
 * <li>the XML file is now available to addon which presents it to the client
 * plugin.</li>
 *
 * </ul>
 *
 * Please refer to the specification files in nuxeo-platform-ws-jaxws/doc/ for
 * details on the format of the nxedit URLs and the XML bootstrap file.
 *
 * @author Thierry Delprat NXP-1959 the bootstrap file is managing the 'create
 *         new document [from template]' case too. The URL is containing an
 *         action identifier.
 * @author Rux rdarlea@nuxeo.com
 * @author Olivier Grisel ogrisel@nuxeo.com (split url functions into JSF
 *         DocumentModelFunctions module)
 *
 */
@Scope(EVENT)
@Name("liveEditHelper")
public class LiveEditBootstrapHelper implements Serializable, LiveEditConstants {

    private static final long serialVersionUID = 876879071L;

    private static final String MODIFIED_FIELD = "modified";

    private static final String DUBLINCORE_SCHEMA = "dublincore";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LiveEditBootstrapHelper.class);

    @In(required = true, create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected CoreSession documentManager;

    @RequestParameter
    private String action;

    @RequestParameter
    private String repoID;

    @RequestParameter
    private String templateRepoID;

    @RequestParameter
    private String docRef;

    @RequestParameter
    private String templateDocRef;

    @RequestParameter
    private String schema;

    @RequestParameter
    private String templateSchema;

    @RequestParameter
    private String blobField;

    @RequestParameter
    private String templateBlobField;

    // to be deprecated once all filenames are stored in the blob itself
    @RequestParameter
    private String filenameField;

    @RequestParameter
    private String mimetype;

    @RequestParameter
    private String docType;

    private CoreSession getSession(String repositoryName)
            throws ClientException {
        RepositoryManager rm;
        try {
            rm = Framework.getService(RepositoryManager.class);
        } catch (Exception e1) {
            throw new ClientException("Unable to get repository Manager:", e1);
        }

        Repository repo = rm.getRepository(repositoryName);
        if (repo == null) {
            throw new ClientException("Unable to get repository "
                    + repositoryName);
        }
        try {
            return repo.open();
        } catch (Exception e1) {
            throw new ClientException("Unable to open session on repository",
                    e1);
        }
    }

    /**
     * Creates the bootstrap file. It is called from the browser's addon. The
     * URL composition tells the case and what to create. The strucuture is
     * depicted in the NXP-1881. Rux NXP-1959: add new tag on root level
     * describing the action: actionEdit, actionNew or actionFromTemplate.
     *
     * @return the bootstrap file content
     * @throws IOException
     * @throws ClientException
     */
    public String getBootstrap() throws IOException, ClientException {

        String currentRepoID = documentManager.getRepositoryName();

        CoreSession session = documentManager;
        CoreSession templateSession = documentManager;
        try {
            if (repoID != null && !currentRepoID.equals(repoID)) {
                session = getSession(repoID);
            }

            if (templateRepoID != null && !currentRepoID.equals(templateRepoID)) {
                templateSession = getSession(templateRepoID);
            }

            DocumentModel doc = null;
            DocumentModel templateDoc = null;
            String filename = null;
            if (ACTION_EDIT_DOCUMENT.equals(action)) {
                // fetch the document to edit to get its mimetype and document
                // type
                doc = session.getDocument(new IdRef(docRef));
                docType = doc.getType();
                Blob blob = (Blob) doc.getProperty(schema, blobField);
                if (blob == null) {
                    throw new ClientException(
                            String.format(
                                    "could not find blob to edit with schema '%s' and field '%s'",
                                    schema, blobField));
                }
                mimetype = blob.getMimeType();
                filename = (String) doc.getProperty(schema, filenameField);
            } else if (ACTION_CREATE_DOCUMENT.equals(action)) {
                // creating a new document all parameters are read from the
                // request parameters
            } else if (ACTION_CREATE_DOCUMENT_FROM_TEMPLATE.equals(action)) {
                // fetch the template blob to get its mimetype
                templateDoc = templateSession.getDocument(new IdRef(
                        templateDocRef));
                Blob blob = (Blob) templateDoc.getProperty(templateSchema,
                        templateBlobField);
                if (blob == null) {
                    throw new ClientException(
                            String.format(
                                    "could not find template blob with schema '%s' and field '%s'",
                                    templateSchema, templateBlobField));
                }
                mimetype = blob.getMimeType();
                // leave docType from the request query parameter
            } else {
                throw new ClientException(
                        String.format(
                                "action '%s' is not a valid LiveEdit action: should be one of '%s', '%s' or '%s'",
                                action, ACTION_CREATE_DOCUMENT,
                                ACTION_CREATE_DOCUMENT_FROM_TEMPLATE,
                                ACTION_EDIT_DOCUMENT));
            }

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

            Element root = DocumentFactory.getInstance().createElement(
                    liveEditTag);
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
            Element docFieldPathT = docInfo.addElement(docfieldPathTag);
            if (schema != null && blobField != null) {
                docFieldPathT.setText(schema + "/" + blobField);
            }
            addTextElement(docInfo, docfileNameTag, filename);
            addTextElement(docInfo, docTypeTag, docType);
            addTextElement(docInfo, docMimetypeTag, mimetype);

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
            docFieldPathT = templateDocInfo.addElement(docfieldPathTag);
            if (templateSchema != null && templateBlobField != null) {
                docFieldPathT.setText(templateSchema + "/" + templateBlobField);
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
            addTextElement(requestInfo, requestBaseURLTag,
                    BaseURL.getBaseURL(request));

            // User related informations
            String username = context.getExternalContext().getUserPrincipal().getName();
            Element userInfo = root.addElement(userInfoTag);
            addTextElement(userInfo, userNameTag, username);
            addTextElement(userInfo, userPasswordTag, "");
            addTextElement(userInfo, userTokenTag, "");
            addTextElement(userInfo, userLocaleTag,
                    context.getViewRoot().getLocale().toString());
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

            // response.setHeader("Content-Disposition", "inline;
            // filename=\"nx5_edit.nuxeo5\");
            response.setContentType("text/xml");
            response.getWriter().write(root.asXML());

            response.flushBuffer();
            context.responseComplete();
            return null;
        } finally {
            if (session != null && session != documentManager) {
                CoreInstance.getInstance().close(session);
            }
            if (templateSession != null && templateSession != documentManager) {
                CoreInstance.getInstance().close(templateSession);
            }
        }
    }

    private static Element addTextElement(Element parent, QName newElementName,
            String value) {
        Element element = parent.addElement(newElementName);
        if (value != null) {
            element.setText(value.toString());
        }
        return element;
    }

    // TODO: please explain what is the use of the "editId" tag here
    private static String getEditId(DocumentModel doc, CoreSession session,
            String userName) throws ClientException {
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
            modified = (Calendar) doc.getProperty(DUBLINCORE_SCHEMA,
                    MODIFIED_FIELD);
        }
        if (modified == null) {
            modified = Calendar.getInstance();
        }
        sb.append('-');
        sb.append(modified.getTimeInMillis());
        return sb.toString();
    }

}
