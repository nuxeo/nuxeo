/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.tag.fn;

import org.dom4j.DocumentFactory;
import org.dom4j.QName;

public interface LiveEditConstants {

    // nxedit query parameter names

    String ACTION = "action";

    String DOC_TYPE = "docType";

    /**
     * @deprecated use {@link #FILENAME_PROPERTY_NAME}
     */
    @Deprecated
    String FILENAME_FIELD = "filenameField";

    /**
     * @deprecated use {@link #BLOB_PROPERTY_NAME}
     */
    @Deprecated
    String BLOB_FIELD = "blobField";

    /**
     * @deprecated use {@link #FILENAME_PROPERTY_NAME} and {@link #BLOB_PROPERTY_NAME}
     */
    @Deprecated
    String SCHEMA = "schema";

    String FILENAME_PROPERTY_NAME = "filenamePropertyName";

    String BLOB_PROPERTY_NAME = "blobPropertyName";

    String MIMETYPE = "mimetype";

    String REPO_ID = "repoID";

    String DOC_REF = "docRef";

    String TEMPLATE_BLOB_FIELD = "templateBlobField";

    String TEMPLATE_SCHEMA = "templateSchema";

    String TEMPLATE_DOC_REF = "templateDocRef";

    String TEMPLATE_REPO_ID = "templateRepoID";

    // action values

    String ACTION_EDIT_DOCUMENT = "edit";

    String ACTION_CREATE_DOCUMENT = "create";

    String ACTION_CREATE_DOCUMENT_FROM_TEMPLATE = "createFromTemplate";

    // default fields to store LiveEditable blobs and related fields

    String DEFAULT_DOCTYPE = "File";

    // to be deprecated once all filenames are stored in the blob itself
    /**
     * @deprecated since 9.1 filename is now stored in the blob itself
     */
    @Deprecated
    String DEFAULT_FILENAME_FIELD = "filename";

    String DEFAULT_BLOB_FIELD = "content";

    String DEFAULT_SUB_BLOB_FIELD = "file";

    String DEFAULT_SCHEMA = "file";

    // XML QNames for the Bootstrap XML and RESTful web service responses

    String XML_LE_NAMESPACE = "http://www.nuxeo.org/liveEdit";

    QName actionSelectorTag = DocumentFactory.getInstance().createQName("actionSelector");

    QName liveEditTag = DocumentFactory.getInstance().createQName("liveEdit");

    QName editIdTag = DocumentFactory.getInstance().createQName("editId");

    QName documentTag = DocumentFactory.getInstance().createQName("document");

    QName templateDocumentTag = DocumentFactory.getInstance().createQName("template");

    QName docRefTag = DocumentFactory.getInstance().createQName("docRef");

    QName docTitleTag = DocumentFactory.getInstance().createQName("docTitle");

    QName docPathTag = DocumentFactory.getInstance().createQName("docPath");

    QName docRepositoryTag = DocumentFactory.getInstance().createQName("repository");

    /**
     * @deprecated use docBlobFieldNameTag instead
     */
    @Deprecated
    QName docFieldNameTag = DocumentFactory.getInstance().createQName("fieldName");

    /**
     * @deprecated use docBlobFieldPathTag instead
     */
    @Deprecated
    QName docfieldPathTag = DocumentFactory.getInstance().createQName("fieldPath");

    QName docBlobFieldNameTag = DocumentFactory.getInstance().createQName("blobFieldName");

    QName docBlobFieldPathTag = DocumentFactory.getInstance().createQName("blobFieldPath");

    QName docFilenameFieldNameTag = DocumentFactory.getInstance().createQName("filenameFieldName");

    QName docFilenameFieldPathTag = DocumentFactory.getInstance().createQName("filenameFieldPath");

    QName docSchemaNameTag = DocumentFactory.getInstance().createQName("schemaName");

    QName docfileNameTag = DocumentFactory.getInstance().createQName("fileName");

    QName docTypeTag = DocumentFactory.getInstance().createQName("type");

    QName docMimetypeTag = DocumentFactory.getInstance().createQName("mimetype");

    QName docFileExtensionTag = DocumentFactory.getInstance().createQName("fileExtension");

    QName docFileAuthorizedExtensionsTag = DocumentFactory.getInstance().createQName(
            "authorizedExtensions");

    QName docFileAuthorizedExtensionTag = DocumentFactory.getInstance().createQName("extension");

    QName docIsVersionTag = DocumentFactory.getInstance().createQName("isVersion");

    QName docIsLockedTag = DocumentFactory.getInstance().createQName("isLocked");

    QName requestInfoTag = DocumentFactory.getInstance().createQName("requestInfo");

    QName requestCookiesTag = DocumentFactory.getInstance().createQName("cookies");

    QName requestCookieTag = DocumentFactory.getInstance().createQName("cookie");

    QName requestHeadersTag = DocumentFactory.getInstance().createQName("headers");

    QName requestHeaderTag = DocumentFactory.getInstance().createQName("header");

    QName requestBaseURLTag = DocumentFactory.getInstance().createQName("baseURL");

    QName userInfoTag = DocumentFactory.getInstance().createQName("userInfo");

    QName userNameTag = DocumentFactory.getInstance().createQName("userName");

    QName userPasswordTag = DocumentFactory.getInstance().createQName("userPassword");

    QName userTokenTag = DocumentFactory.getInstance().createQName("userToken");

    QName userLocaleTag = DocumentFactory.getInstance().createQName("userLocale");

    QName wsdlLocationsTag = DocumentFactory.getInstance().createQName("wsdlLocations");

    QName wsdlAccessWebServiceTag = DocumentFactory.getInstance().createQName("wsdlAccessWS");

    QName wsdlLEWebServiceTag = DocumentFactory.getInstance().createQName("wsdlLiveEditWS");

    QName serverInfoTag = DocumentFactory.getInstance().createQName("serverInfo");

    QName serverVersionTag = DocumentFactory.getInstance().createQName("serverVersion");

    String URL_ENCODE_CHARSET = "UTF-8";

}
