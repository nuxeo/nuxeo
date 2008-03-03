package org.nuxeo.ecm.platform.ui.web.tag.fn;

import org.dom4j.DocumentFactory;
import org.dom4j.QName;

public interface LiveEditConstants {

    // nxedit query parameter names

    public static final String ACTION = "action";

    public static final String DOC_TYPE = "docType";

    public static final String FILENAME_FIELD = "filenameField";

    public static final String BLOB_FIELD = "blobField";

    public static final String SCHEMA = "schema";

    public static final String MIMETYPE = "mimetype";

    public static final String REPO_ID = "repoID";

    public static final String DOC_REF = "docRef";

    public static final String TEMPLATE_BLOB_FIELD = "templateBlobField";

    public static final String TEMPLATE_SCHEMA = "templateSchema";

    public static final String TEMPLATE_DOC_REF = "templateDocRef";

    public static final String TEMPLATE_REPO_ID = "templateRepoID";

    // action values

    public static final String ACTION_EDIT_DOCUMENT = "edit";

    public static final String ACTION_CREATE_DOCUMENT = "create";

    public static final String ACTION_CREATE_DOCUMENT_FROM_TEMPLATE = "createFromTemplate";

    // default fields to store LiveEditable blobs and related fields

    public static final String DEFAULT_DOCTYPE = "File";

    // to be deprecated once all filenames are stored in the blob itself
    public static final String DEFAULT_FILENAME_FIELD = "filename";

    public static final String DEFAULT_BLOB_FIELD = "content";

    public static final String DEFAULT_SCHEMA = "file";

    // XML QNames for the Bootstrap XML and RESTful web service responses

    public static final String XML_LE_NAMESPACE = "http://www.nuxeo.org/liveEdit";

    public static final QName actionSelectorTag = DocumentFactory.getInstance().createQName(
            "actionSelector");

    public static final QName liveEditTag = DocumentFactory.getInstance().createQName(
            "liveEdit");

    public static final QName editIdTag = DocumentFactory.getInstance().createQName(
            "editId");

    public static final QName documentTag = DocumentFactory.getInstance().createQName(
            "document");

    public static final QName templateDocumentTag = DocumentFactory.getInstance().createQName(
            "template");

    public static final QName docRefTag = DocumentFactory.getInstance().createQName(
            "docRef");

    public static final QName docTitleTag = DocumentFactory.getInstance().createQName(
            "docTitle");

    public static final QName docPathTag = DocumentFactory.getInstance().createQName(
            "docPath");

    public static final QName docRepositoryTag = DocumentFactory.getInstance().createQName(
            "repository");

    public static final QName docFieldNameTag = DocumentFactory.getInstance().createQName(
            "fieldName");

    public static final QName docSchemaNameTag = DocumentFactory.getInstance().createQName(
            "schemaName");

    public static final QName docfieldPathTag = DocumentFactory.getInstance().createQName(
            "fieldPath");

    public static final QName docfileNameTag = DocumentFactory.getInstance().createQName(
            "fileName");

    public static final QName docTypeTag = DocumentFactory.getInstance().createQName(
            "type");

    public static final QName docMimetypeTag = DocumentFactory.getInstance().createQName(
            "mimetype");

    public static final QName docIsVersionTag = DocumentFactory.getInstance().createQName(
            "isVersion");

    public static final QName docIsLockedTag = DocumentFactory.getInstance().createQName(
            "isLocked");

    public static final QName requestInfoTag = DocumentFactory.getInstance().createQName(
            "requestInfo");

    public static final QName requestCookiesTag = DocumentFactory.getInstance().createQName(
            "cookies");

    public static final QName requestCookieTag = DocumentFactory.getInstance().createQName(
            "cookie");

    public static final QName requestHeadersTag = DocumentFactory.getInstance().createQName(
            "headers");

    public static final QName requestHeaderTag = DocumentFactory.getInstance().createQName(
            "header");

    public static final QName requestBaseURLTag = DocumentFactory.getInstance().createQName(
            "baseURL");

    public static final QName userInfoTag = DocumentFactory.getInstance().createQName(
            "userInfo");

    public static final QName userNameTag = DocumentFactory.getInstance().createQName(
            "userName");

    public static final QName userPasswordTag = DocumentFactory.getInstance().createQName(
            "userPassword");

    public static final QName userTokenTag = DocumentFactory.getInstance().createQName(
            "userToken");

    public static final QName userLocaleTag = DocumentFactory.getInstance().createQName(
            "userLocale");

    public static final QName wsdlLocationsTag = DocumentFactory.getInstance().createQName(
            "wsdlLocations");

    public static final QName wsdlAccessWebServiceTag = DocumentFactory.getInstance().createQName(
            "wsdlAccessWS");

    public static final QName wsdlLEWebServiceTag = DocumentFactory.getInstance().createQName(
            "wsdlLiveEditWS");

    public static final QName serverInfoTag = DocumentFactory.getInstance().createQName(
            "serverInfo");

    public static final QName serverVersionTag = DocumentFactory.getInstance().createQName(
            "serverVersion");

}
