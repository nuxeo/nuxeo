/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.tag;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility class used to perform the actions specific to tagging. It will be
 * used both in the JSF backoffice UI and webengine UI.
 *
 * @author btatar
 *
 */
public class TaggingHelper {

    private static final Log log = LogFactory.getLog(TaggingHelper.class);

    private TagService tagService;

    public static boolean isStringBlank(String string) {
        if  (string == null || string.trim().length() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isTagServiceEnabled() throws ClientException {
        return getTagService().isEnabled();
    }

    /**
     * Tags the received <b>document</b> parameter.
     *
     * @param session - the Nuxeo core session
     * @param document - the document model that will be tagged
     * @param tagLabel - the label of the tagging
     * @throws ClientException
     */
    public void addTagging(CoreSession session, DocumentModel document,
            String tagLabel) throws ClientException {

        if (isStringBlank(tagLabel)) {
            log.debug("The information provided about the new tag(s) cannot be empty ...");
            return;
        }

        if (null == document) {
            throw new ClientException("Can not tag document null.");
        }

        // there could be more tags provided, which are separated by ','
        String[] tagLabelArray = tagLabel.split(",");
        DocumentModel rootTag = getTagService().getRootTag(session);
        if (rootTag == null) {
            log.debug("The root tag document was not properly generated ...");
            return;
        }
        for (int i = 0; i < tagLabelArray.length; i++) {
            String currentTagLabel = tagLabelArray[i].trim();
            if (currentTagLabel.length() > 0) {
                DocumentModel tagDocument = getTagService().getOrCreateTag(
                        session, rootTag, currentTagLabel, false);
                getTagService().tagDocument(session, document,
                        tagDocument.getId(), false);
            }
        }
    }

    /**
     * Removes tagging from a document.
     *
     * @param session - the Nuxeo core session
     * @param document - the document model from which the tagging will be
     *            removed
     * @param taggingId - the id of the tagging that will be removed
     * @throws ClientException
     */
    public void removeTagging(CoreSession session, DocumentModel document,
            String taggingId) throws ClientException {

        if (isStringBlank(taggingId)) {
            log.debug("The taggingId cannot be empty ...");
            return;
        }

        if (null == document) {
            throw new ClientException(
                    "Can not remove tag(s) from document null.");
        }

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        if (principal.isAdministrator()
                || session.hasPermission(document.getRef(),
                        SecurityConstants.WRITE)) {
            getTagService().completeUntagDocument(session, document, taggingId);
        } else {
            getTagService().untagDocument(session, document, taggingId);
        }

    }

    /**
     * Returns the list with distinct public tags (or owned by user) that are
     * applied on received <b>document</b> parameter.
     *
     * @param session - the Nuxeo core session
     * @param document - the document for which to retrieve the list with tags
     * @return
     * @throws ClientException
     */
    public List<Tag> listDocumentTags(CoreSession session,
            DocumentModel document) throws ClientException {
        if (null == document) {
            throw new ClientException("Can not list tag(s) on document null.");
        }
        return getTagService().listTagsAppliedOnDocument(session, document);
    }

    /**
     * Returns the details about the tag cloud that have been created under a
     * document.
     *
     * @param session - the Nuxeo core session
     * @param document
     * @return
     * @throws ClientException
     */
    public List<WeightedTag> getPopularCloud(CoreSession session,
            DocumentModel document) throws ClientException {
        if (null == document) {
            throw new ClientException(
                    "Can not retrieve popular cloud on document null.");
        }
        return getTagService().getPopularCloud(session, document);
    }


    public List<WeightedTag> getPopularCloudOnAllDocuments(CoreSession session) throws ClientException {
        return getTagService().getPopularCloudOnAllDocuments(session);
    }


    /**
     * Returns the list with documents which are tagged with a particular tag
     * that has the received <b>tagDocumentId</b> id.
     *
     * @param session - the nuxeo core session
     * @param tagDocumentId - the id of the tag
     * @return
     * @throws ClientException
     */
    public DocumentModelList listDocumentsForTag(CoreSession session,
            String tagDocumentId) throws ClientException {
        if (isStringBlank(tagDocumentId)) {
            throw new ClientException(
                    "Can not list documents for an empty tag .");
        }
        DocumentModelList documentsForTag = new DocumentModelListImpl();

        List<String> docsForTag = tagService.listDocumentsForTag(session,
                tagDocumentId, session.getPrincipal().getName());
        for (String docForTagId : docsForTag) {
            documentsForTag.add(session.getDocument(new IdRef(docForTagId)));
        }
        return documentsForTag;
    }

    /**
     * Specify whether the current logged user has enough rights to modify a tag
     * that is applied on the received <b>document</b> parameter
     *
     * @param session - the Nuxeo core session
     * @param document - the document on which the tag was applied
     * @param tag - the tag that is applied on the document
     * @return
     * @throws ClientException
     */
    public boolean canModifyTag(CoreSession session, DocumentModel document,
            Tag tag) throws ClientException {
        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        if (principal.isAdministrator()
                || session.hasPermission(document.getRef(),
                        SecurityConstants.WRITE)) {
            return true;
        }
        return tagService.getTaggingId(session, document.getId(), tag.tagLabel,
                principal.getName()) != null;
    }

    /**
     * Returns the TagService service which is provided by Nuxeo.
     *
     * @return
     */
    private TagService getTagService() {
        if (tagService == null) {
            try {
                tagService = Framework.getService(TagService.class);
            } catch (Exception e) {
                log.debug("Problems retrieving the TagService ...", e);
                throw new IllegalStateException(
                        "TagService service not deployed.", e);
            }
        }
        return tagService;
    }
}
