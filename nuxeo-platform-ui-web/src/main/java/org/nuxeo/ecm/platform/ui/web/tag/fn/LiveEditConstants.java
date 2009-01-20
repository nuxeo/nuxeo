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

package org.nuxeo.ecm.platform.ui.web.tag.fn;

import org.dom4j.DocumentFactory;
import org.dom4j.QName;

public interface LiveEditConstants {

    // nxedit query parameter names

    static final String ACTION = "action";

    static final String DOC_TYPE = "docType";

    /**
     * @deprecated use {@link #FILENAME_PROPERTY_NAME}
     */
    @Deprecated
    static final String FILENAME_FIELD = "filenameField";

    /**
     * @deprecated use {@link #BLOB_PROPERTY_NAME}
     */
    @Deprecated
    static final String BLOB_FIELD = "blobField";

    /**
     * @deprecated use {@link #FILENAME_PROPERTY_NAME} and
     *             {@link #BLOB_PROPERTY_NAME}
     */
    @Deprecated
    static final String SCHEMA = "schema";

    static final String FILENAME_PROPERTY_NAME = "filenamePropertyName";

    static final String BLOB_PROPERTY_NAME = "blobPropertyName";

    static final String MIMETYPE = "mimetype";

    static final String REPO_ID = "repoID";

    static final String DOC_REF = "docRef";

    static final String TEMPLATE_BLOB_FIELD = "templateBlobField";

    static final String TEMPLATE_SCHEMA = "templateSchema";

    static final String TEMPLATE_DOC_REF = "templateDocRef";

    static final String TEMPLATE_REPO_ID = "templateRepoID";

    // action values

    static final String ACTION_EDIT_DOCUMENT = "edit";

    static final String ACTION_CREATE_DOCUMENT = "create";

    static final String ACTION_CREATE_DOCUMENT_FROM_TEMPLATE = "createFromTemplate";

    // default fields to store LiveEditable blobs and related fields

    static final String DEFAULT_DOCTYPE = "File";

    // to be deprecated once all filenames are stored in the blob itself
    static final String DEFAULT_FILENAME_FIELD = "filename";

    static final String DEFAULT_BLOB_FIELD = "content";

    static final String DEFAULT_SUB_BLOB_FIELD = "file";

    static final String DEFAULT_SCHEMA = "file";

    // XML QNames for the Bootstrap XML and RESTful web service responses

    static final String XML_LE_NAMESPACE = "http://www.nuxeo.org/liveEdit";

    static final QName actionSelectorTag = DocumentFactory.getInstance().createQName(
            "actionSelector");

    static final QName liveEditTag = DocumentFactory.getInstance().createQName(
            "liveEdit");

    static final QName editIdTag = DocumentFactory.getInstance().createQName(
            "editId");

    static final QName documentTag = DocumentFactory.getInstance().createQName(
            "document");

    static final QName templateDocumentTag = DocumentFactory.getInstance().createQName(
            "template");

    static final QName docRefTag = DocumentFactory.getInstance().createQName(
            "docRef");

    static final QName docTitleTag = DocumentFactory.getInstance().createQName(
            "docTitle");

    static final QName docPathTag = DocumentFactory.getInstance().createQName(
            "docPath");

    static final QName docRepositoryTag = DocumentFactory.getInstance().createQName(
            "repository");

    /**
     * @deprecated use docBlobFieldNameTag instead
     */
    @Deprecated
    static final QName docFieldNameTag = DocumentFactory.getInstance().createQName(
            "fieldName");

    /**
     * @deprecated use docBlobFieldPathTag instead
     */
    @Deprecated
    static final QName docfieldPathTag = DocumentFactory.getInstance().createQName(
            "fieldPath");

    static final QName docBlobFieldNameTag = DocumentFactory.getInstance().createQName(
            "blobFieldName");

    static final QName docBlobFieldPathTag = DocumentFactory.getInstance().createQName(
            "blobFieldPath");

    static final QName docFilenameFieldNameTag = DocumentFactory.getInstance().createQName(
            "filenameFieldName");

    static final QName docFilenameFieldPathTag = DocumentFactory.getInstance().createQName(
            "filenameFieldPath");

    static final QName docSchemaNameTag = DocumentFactory.getInstance().createQName(
            "schemaName");

    static final QName docfileNameTag = DocumentFactory.getInstance().createQName(
            "fileName");

    static final QName docTypeTag = DocumentFactory.getInstance().createQName(
            "type");

    static final QName docMimetypeTag = DocumentFactory.getInstance().createQName(
            "mimetype");

    static final QName docFileExtensionTag = DocumentFactory.getInstance().createQName(
            "fileExtension");

    static final QName docFileAuthorizedExtensionsTag = DocumentFactory.getInstance().createQName(
            "authorizedExtensions");

    static final QName docFileAuthorizedExtensionTag = DocumentFactory.getInstance().createQName(
            "extension");

    static final QName docIsVersionTag = DocumentFactory.getInstance().createQName(
            "isVersion");

    static final QName docIsLockedTag = DocumentFactory.getInstance().createQName(
            "isLocked");

    static final QName requestInfoTag = DocumentFactory.getInstance().createQName(
            "requestInfo");

    static final QName requestCookiesTag = DocumentFactory.getInstance().createQName(
            "cookies");

    static final QName requestCookieTag = DocumentFactory.getInstance().createQName(
            "cookie");

    static final QName requestHeadersTag = DocumentFactory.getInstance().createQName(
            "headers");

    static final QName requestHeaderTag = DocumentFactory.getInstance().createQName(
            "header");

    static final QName requestBaseURLTag = DocumentFactory.getInstance().createQName(
            "baseURL");

    static final QName userInfoTag = DocumentFactory.getInstance().createQName(
            "userInfo");

    static final QName userNameTag = DocumentFactory.getInstance().createQName(
            "userName");

    static final QName userPasswordTag = DocumentFactory.getInstance().createQName(
            "userPassword");

    static final QName userTokenTag = DocumentFactory.getInstance().createQName(
            "userToken");

    static final QName userLocaleTag = DocumentFactory.getInstance().createQName(
            "userLocale");

    static final QName wsdlLocationsTag = DocumentFactory.getInstance().createQName(
            "wsdlLocations");

    static final QName wsdlAccessWebServiceTag = DocumentFactory.getInstance().createQName(
            "wsdlAccessWS");

    static final QName wsdlLEWebServiceTag = DocumentFactory.getInstance().createQName(
            "wsdlLiveEditWS");

    static final QName serverInfoTag = DocumentFactory.getInstance().createQName(
            "serverInfo");

    static final QName serverVersionTag = DocumentFactory.getInstance().createQName(
            "serverVersion");

    static final String URL_ENCODE_CHARSET = "UTF-8";

}
