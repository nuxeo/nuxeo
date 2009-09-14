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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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

        if (StringUtils.isEmpty(tagLabel)) {
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

        if (StringUtils.isEmpty(taggingId)) {
            log.debug("The taggingId cannot be empty ...");
            return;
        }

        if (null == document) {
            throw new ClientException(
                    "Can not remove tag(s) from document null.");
        }

        if (((NuxeoPrincipal) session.getPrincipal()).isAdministrator()) {
            getTagService().completeUntagDocument(document, taggingId);
        } else {
            getTagService().untagDocument(document, taggingId);
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
        if (principal.isAdministrator()) {
            return true;
        }
        if (!session.hasPermission(document.getRef(), "Write")) {
            return false;
        }
        return tagService.getTaggingId(tag.tagId, tag.tagLabel,
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
