/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.PAGE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.service.DiffDisplayService;
import org.nuxeo.ecm.diff.service.DocumentDiffService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.versioning.VersionedActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles document diff actions.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@Name("diffActions")
@Scope(CONVERSATION)
public class DiffActionsBean implements Serializable {

    private static final long serialVersionUID = -5507491210664361778L;

    private static final Log log = LogFactory.getLog(DiffActionsBean.class);

    private static final String DOC_DIFF_VIEW = "view_doc_diff";

    private static final String CONTENT_DIFF_DIFFERENCE_TYPE_MSG_KEY_PREFIX = "diff.content.differenceType.message.";

    private static final String LAST_VERSION_PROPERTY = "lastVersion";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient VersionedActions versionedActions;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected DocumentModel leftDoc;

    protected DocumentModel rightDoc;

    protected String selectedVersionId;

    protected String diffSelectionType = DiffSelectionType.content.name();

    /**
     * Checks if the diff action is available for the {@link DocumentsListsManager#CURRENT_DOCUMENT_SELECTION} working
     * list.
     *
     * @return true if can diff the current document selection
     */
    public boolean getCanDiffCurrentDocumentSelection() {

        return getCanDiffWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    /**
     * Checks if the diff action is available for the {@link DocumentsListsManager#CURRENT_DOCUMENT_TRASH_SELECTION}
     * working list.
     *
     * @return true if can diff the current document trash selection
     */
    public boolean getCanDiffCurrentTrashSelection() {

        return getCanDiffWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION);
    }

    /**
     * Checks if the diff action is available for the {@link DocumentsListsManager#CURRENT_DOCUMENT_SECTION_SELECTION}
     * working list.
     *
     * @return true if can diff the current section selection
     */
    public boolean getCanDiffCurrentSectionSelection() {

        return getCanDiffWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);
    }

    /**
     * Checks if the diff action is available for the {@link VersionDocumentsListsConstants#CURRENT_VERSION_SELECTION}
     * working list.
     *
     * @return true if can diff the current version selection
     */
    public boolean getCanDiffCurrentVersionSelection() {

        return getCanDiffWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION);
    }

    /**
     * Checks if the diff action is available for the {@link DocumentsListsManager#DEFAULT_WORKING_LIST} working list.
     *
     * @return true if can diff the current default working list selection
     */
    public boolean getCanDiffCurrentDefaultSelection() {

        return getCanDiffWorkingList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    /**
     * Checks if the diff action is available for the {@code listName} working list.
     * <p>
     * Condition: the working list has exactly 2 documents.
     *
     * @param listName the list name
     * @return true if can diff the {@code listName} working list
     */
    public boolean getCanDiffWorkingList(String listName) {

        List<DocumentModel> currentSelectionWorkingList = documentsListsManager.getWorkingList(listName);
        return currentSelectionWorkingList != null && currentSelectionWorkingList.size() == 2;
    }

    /**
     * Prepares a diff of the current document selection.
     *
     * @return the view id
     */
    public String prepareCurrentDocumentSelectionDiff() {

        diffSelectionType = DiffSelectionType.content.name();
        return prepareWorkingListDiff(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    /**
     * Prepares a diff of the current document trash selection.
     *
     * @return the view id
     */
    public String prepareCurrentTrashSelectionDiff() {

        diffSelectionType = DiffSelectionType.trash.name();
        return prepareWorkingListDiff(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION);
    }

    /**
     * Prepares a diff of the current section selection.
     *
     * @return the view id
     */
    public String prepareCurrentSectionSelectionDiff() {

        diffSelectionType = DiffSelectionType.content.name();
        return prepareWorkingListDiff(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);
    }

    /**
     * Prepares a diff of the current version selection.
     *
     * @return the view id
     */
    public String prepareCurrentVersionSelectionDiff() {

        diffSelectionType = DiffSelectionType.version.name();
        return prepareWorkingListDiff(DocumentsListsManager.CURRENT_VERSION_SELECTION);
    }

    /**
     * Prepares a diff of the current default selection.
     *
     * @return the view id
     */
    public String prepareCurrentDefaultSelectionDiff() {

        diffSelectionType = DiffSelectionType.content.name();
        return prepareWorkingListDiff(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    /**
     * Prepares a diff of the {@code listName} working list.
     *
     * @return the view id
     */
    public String prepareWorkingListDiff(String listName) {

        List<DocumentModel> workingList = getWorkingList(listName);

        leftDoc = workingList.get(0);
        rightDoc = workingList.get(1);

        return refresh();
    }

    /**
     * Prepare a diff of the current document with a specific version
     *
     * @param versionLabel version label to look for, if you want the last version use org.nuxeo.ecm.diff.web
     *            .DiffActionsBean#LAST_VERSION_PROPERTY
     */
    public String prepareCurrentVersionDiff(String versionLabel) {
        if (StringUtils.isBlank(versionLabel)) {
            versionLabel = LAST_VERSION_PROPERTY;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument.isVersion()) {
            log.info("Unable to diff, current document is a version document");
            return null;
        }

        DocumentModel documentVersion;
        if (LAST_VERSION_PROPERTY.equals(versionLabel)) {
            documentVersion = documentManager.getLastDocumentVersion(currentDocument.getRef());

            if (documentVersion == null) {
                log.info("Unable to diff, current document do not have any versions yet.");
                return null;
            }
        } else {
            VersionModel versionModel = new VersionModelImpl();
            versionModel.setLabel(versionLabel);
            documentVersion = documentManager.getDocumentWithVersion(currentDocument.getRef(), versionModel);

            if (documentVersion == null) {
                log.info("Unable to found " + versionLabel + " on current document to diff.");
                return null;
            }
        }

        setLeftDoc(currentDocument);
        setRightDoc(documentVersion);

        diffSelectionType = DiffSelectionType.version.name();

        return DOC_DIFF_VIEW;
    }

    /**
     * Prepares a diff of the selected version with the live doc.
     *
     * @return the view id
     */
    public String prepareCurrentVersionDiff() {

        String selectedVersionId = versionedActions.getSelectedVersionId();
        if (selectedVersionId != null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument == null) {
                throw new NuxeoException(
                        "Cannot make a diff between selected version and current document since current document is null.");
            }

            VersionModel selectedVersion = new VersionModelImpl();
            selectedVersion.setId(selectedVersionId);
            DocumentModel docVersion = documentManager.getDocumentWithVersion(currentDocument.getRef(), selectedVersion);
            if (docVersion == null) {
                throw new NuxeoException(
                        "Cannot make a diff between selected version and current document since selected version document is null.");
            }

            leftDoc = docVersion;
            rightDoc = currentDocument;

            diffSelectionType = DiffSelectionType.version.name();

            return DOC_DIFF_VIEW;
        }
        return null;
    }

    /**
     * Refreshes the diff between leftDoc and rightDoc.
     *
     * @return the view id
     */
    public String refresh() {

        // Fetch docs from repository
        if (isDocumentDiffAvailable()) {
            leftDoc = documentManager.getDocument(leftDoc.getRef());
            rightDoc = documentManager.getDocument(rightDoc.getRef());
        }

        return DOC_DIFF_VIEW;
    }

    /**
     * Checks if document diff is available.
     *
     * @return true, if is document diff available
     */
    public boolean isDocumentDiffAvailable() {
        return leftDoc != null && rightDoc != null;
    }

    /**
     * Gets the document diff.
     *
     * @return the document diff between leftDoc and rightDoc if leftDoc and rightDoc aren't null, else null
     */
    @Factory(value = "defaultDiffDisplayBlocks", scope = PAGE)
    public List<DiffDisplayBlock> getDefaultDiffDisplayBlocks() {

        if (leftDoc == null || rightDoc == null) {
            return new ArrayList<>();
        }

        DocumentDiff docDiff = getDocumentDiffService().diff(documentManager, leftDoc, rightDoc);
        return getDiffDisplayService().getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
    }

    /**
     * Gets the content diff fancybox URL for the property with xpath {@code propertyXPath}.
     *
     * @param propertyLabel the property label
     * @param propertyXPath the property xpath
     * @return the content diff fancybox URL
     */
    public String getContentDiffFancyBoxURL(String propertyLabel, String propertyXPath) {

        return getContentDiffFancyBoxURL(propertyLabel, propertyXPath, null);
    }

    /**
     * Gets the content diff fancybox URL for the property with xpath {@code propertyXPath} using {@code conversionType}
     * .
     *
     * @param propertyLabel the property label
     * @param propertyXPath the property xpath
     * @param conversionType the conversion type
     * @return the content diff fancybox URL
     */
    public String getContentDiffFancyBoxURL(String propertyLabel, String propertyXPath, String conversionType)
            {

        if (StringUtils.isEmpty(propertyXPath)) {
            log.error("Cannot get content diff fancybox URL with a null propertyXPath.");
            return null;
        }
        return getContentDiffFancyBoxURL(navigationContext.getCurrentDocument(), propertyLabel, propertyXPath,
                conversionType);
    }

    /**
     * Gets the content diff fancy box URL.
     *
     * @param currentDoc the current doc
     * @param propertyLabel the property label
     * @param propertyXPath the property xpath
     * @param conversionType the conversion type
     * @return the content diff fancy box URL
     */
    public static String getContentDiffFancyBoxURL(DocumentModel currentDoc, String propertyLabel, String propertyXPath,
            String conversionType) {
        DocumentLocation docLocation = new DocumentLocationImpl(currentDoc.getRepositoryName(), currentDoc.getRef());
        DocumentView docView = new DocumentViewImpl(docLocation, ContentDiffHelper.CONTENT_DIFF_FANCYBOX_VIEW);
        docView.setPatternName("id");
        URLPolicyService urlPolicyService = Framework.getService(URLPolicyService.class);
        String docUrl = urlPolicyService.getUrlFromDocumentView(docView, VirtualHostHelper.getContextPathProperty());
        if (docUrl == null) {
            throw new NuxeoException(
                    "Cannot get URL from document view, probably because of a missing urlPattern contribution.");
        }
        Map<String, String> requestParams = new LinkedHashMap<>();
        requestParams.put(ContentDiffHelper.LABEL_URL_PARAM_NAME, propertyLabel);
        requestParams.put(ContentDiffHelper.XPATH_URL_PARAM_NAME, propertyXPath);
        if (!StringUtils.isEmpty(conversionType)) {
            requestParams.put(ContentDiffHelper.CONVERSION_TYPE_URL_PARAM_NAME, conversionType);
        }
        docUrl = URIUtils.addParametersToURIQuery(docUrl, requestParams);
        return RestHelper.addCurrentConversationParameters(docUrl);
    }

    /**
     * Gets the content diff URL of two documents independently of the current document
     *
     * @param docLeftId a DocumentModel id, not a path.
     * @param docRightId a DocumentModel id, not a path.
     */
    public String getContentDiffURL(String docLeftId, String docRightId, String propertyXPath,
            String conversionTypeParam) {
        DocumentModel leftDoc = null;
        DocumentModel rightDoc = null;
        if (!StringUtils.isBlank(docLeftId)) {
            leftDoc = documentManager.getDocument(new IdRef(docLeftId));
        }

        if (!StringUtils.isBlank(docRightId)) {
            rightDoc = documentManager.getDocument(new IdRef(docRightId));
        }

        if (rightDoc == null || leftDoc == null) {
            log.error("Cannot get content diff URL with a null leftDoc or a null rightDoc.");
            return null;
        }

        if (StringUtils.isEmpty(propertyXPath)) {
            log.error("Cannot get content diff URL with a null schemaName or a null fieldName.");
            return null;
        }

        String conversionType = null;
        if (!StringUtils.isEmpty(conversionTypeParam)) {
            conversionType = conversionTypeParam;
        }

        return ContentDiffHelper.getContentDiffURL(navigationContext.getCurrentDocument().getRepositoryName(), leftDoc,
                rightDoc, propertyXPath, conversionType, localeSelector.getLocaleString());
    }

    /**
     * Gets the content diff URL.
     *
     * @param propertyXPath the property xpath
     * @param conversionTypeParam the conversion type param
     * @return the content diff URL
     */
    public String getContentDiffURL(String propertyXPath, String conversionTypeParam) {

        if (leftDoc == null || rightDoc == null) {
            log.error("Cannot get content diff URL with a null leftDoc or a null rightDoc.");
            return null;
        }
        if (StringUtils.isEmpty(propertyXPath)) {
            log.error("Cannot get content diff URL with a null schemaName or a null fieldName.");
            return null;
        }
        String conversionType = null;
        if (!StringUtils.isEmpty(conversionTypeParam)) {
            conversionType = conversionTypeParam;
        }
        return ContentDiffHelper.getContentDiffURL(navigationContext.getCurrentDocument().getRepositoryName(), leftDoc,
                rightDoc, propertyXPath, conversionType, localeSelector.getLocaleString());
    }

    /**
     * Gets the content diff with blob post processing URL.
     *
     * @param propertyXPath the property xpath
     * @param conversionTypeParam the conversion type param
     * @return the content diff with blob post processing URL
     */
    public String getContentDiffWithBlobPostProcessingURL(String propertyXPath, String conversionTypeParam) {
        return getContentDiffURL(propertyXPath, conversionTypeParam) + "?blobPostProcessing=true";
    }

    /**
     * Checks if is different filename.
     */
    public boolean isDifferentFilename(DifferenceType differenceType) {
        return DifferenceType.differentFilename.equals(differenceType);
    }

    /**
     * Gets the content diff difference type message key.
     */
    public String getContentDiffDifferenceTypeMsgKey(DifferenceType differenceType) {
        return CONTENT_DIFF_DIFFERENCE_TYPE_MSG_KEY_PREFIX + differenceType.name();
    }

    /**
     * Gets the {@code listName} working list.
     *
     * @return the {@code listName} working list
     */
    protected final List<DocumentModel> getWorkingList(String listName) {

        List<DocumentModel> currentSelectionWorkingList = documentsListsManager.getWorkingList(listName);

        if (currentSelectionWorkingList == null || currentSelectionWorkingList.size() != 2) {
            throw new NuxeoException(String.format(
                    "Cannot make a diff of the %s working list: need to have exactly 2 documents in the working list.",
                    listName));
        }
        return currentSelectionWorkingList;
    }

    /**
     * Gets the document diff service.
     *
     * @return the document diff service
     */
    protected final DocumentDiffService getDocumentDiffService() {
        return Framework.getService(DocumentDiffService.class);
    }

    /**
     * Gets the diff display service.
     *
     * @return the diff display service
     */
    protected final DiffDisplayService getDiffDisplayService() {
        return Framework.getService(DiffDisplayService.class);
    }

    public DocumentModel getLeftDoc() {
        return leftDoc;
    }

    public void setLeftDoc(DocumentModel leftDoc) {
        this.leftDoc = leftDoc;
    }

    public DocumentModel getRightDoc() {
        return rightDoc;
    }

    public void setRightDoc(DocumentModel rightDoc) {
        this.rightDoc = rightDoc;
    }

    public String getDiffSelectionType() {
        return diffSelectionType;
    }

    public void setDiffSelectionType(String diffSelectionType) {
        this.diffSelectionType = diffSelectionType;
    }
}
