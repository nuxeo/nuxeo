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
package org.nuxeo.ecm.platform.tag.web;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TaggingHelper;
import org.nuxeo.ecm.platform.tag.WeightedTag;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This Seam bean provides support for tagging related actions which can be made
 * on the current document.
 *
 * @author btatar
 *
 */

@Name("tagActions")
@Scope(CONVERSATION)
public class TagActionsBean implements Serializable {

    public static final String TAG_SEARCH_RESULT_PAGE = "tag_search_results";

    private static final long serialVersionUID = -630033792577162398L;

    private static final Log log = LogFactory.getLog(TagActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected TaggingHelper taggingHelper;

    protected String tagDocumentId;

    /**
     * Keeps the tagging information that will be performed on the current
     * document document.
     */
    private String tagsLabel;

    /**
     * Used for controlling the presence of the tagging text field in UI.
     */
    private Boolean addTag;

    @Create
    public void initialize() throws Exception {
        log.debug("Initializing 'tagActions' Seam component ...");
        taggingHelper = new TaggingHelper();
    }

    @Destroy
    public void destroy() {
        log.debug("Removing 'tagActions' Seam component...");
    }

    /**
     * Returns the list with distinct public tags (or owned by user) that are
     * applied on the current document.
     *
     * @return
     * @throws ClientException
     */
    public List<Tag> getDocumentTags() throws ClientException {
        return taggingHelper.listDocumentTags(documentManager,
                navigationContext.getCurrentDocument());
    }

    /**
     * Performs the tagging on the current document.
     *
     * @return
     * @throws ClientException
     */
    public String addTagging() throws ClientException {
        String messageKey = null;
        if (StringUtils.isBlank(tagsLabel)) {
            messageKey = "message.add.new.tagging.not.empty";
        } else {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            taggingHelper.addTagging(documentManager, currentDocument,
                    tagsLabel);
            messageKey = "message.add.new.tagging";
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(messageKey), tagsLabel);
        reset();
        return null;
    }

    /**
     * Removes a tagging from the current document.
     *
     * @return
     * @throws ClientException
     */
    public String removeTagging(String taggingId) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        taggingHelper.removeTagging(documentManager, currentDocument, taggingId);
        reset();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("message.remove.tagging"),
                taggingId);

        return null;
    }

    /**
     * Returns the details about the tag cloud that have been created under the
     * current document, which should be a Workspace
     *
     * @return
     * @throws ClientException
     */
    public List<WeightedTag> getPopularCloud() throws ClientException {
        List<WeightedTag> tagCloud = new ArrayList<WeightedTag>();
        int min, max;
        min = max = 0;

        for (DocumentModel document : documentManager.getChildren(documentManager.getRootDocument().getRef())) {
            for (WeightedTag weightedTag : taggingHelper.getPopularCloud(document)) {
                if (weightedTag.getWeight() > max) {
                    max = weightedTag.getWeight();
                }
                if (weightedTag.getWeight() < min) {
                    min = weightedTag.getWeight();
                }
                tagCloud.add(weightedTag);
            }
        }
        for (WeightedTag tag : tagCloud) {
            tag.setWeight((int) Math.round((150.0 * (1.0 + (1.5 * tag.getWeight() - min / 2)
                    / max))) / 2);
        }

        return tagCloud;
    }

    public String listDocumentsForTag(String tagDocumentId)
            throws ClientException {
        this.tagDocumentId = tagDocumentId;
        return TAG_SEARCH_RESULT_PAGE;
    }

    @Factory(value = "taggedDocuments", scope = EVENT)
    public DocumentModelList getChildrenSelectModel() throws ClientException {
        DocumentModelList taggedDocuments = new DocumentModelListImpl();
        if (!StringUtils.isBlank(tagDocumentId)) {
            taggedDocuments.addAll(taggingHelper.listDocumentsForTag(
                    documentManager, tagDocumentId));
        }
        return taggedDocuments;
    }

    /**
     * Returns <b>true</b> if the current logged user has permission to modify a
     * tag that is applied on the current document.
     *
     * @param tag
     * @return
     * @throws ClientException
     */
    public boolean canModifyTag(Tag tag) throws ClientException {
        return taggingHelper.canModifyTag(documentManager,
                navigationContext.getCurrentDocument(), tag);
    }

    /**
     * Resets the fields that are used for managing actions related to tagging.
     */
    public void reset() {
        tagsLabel = null;
    }

    /**
     * Used to decide whether the tagging UI field is shown or not.
     *
     * @param event
     */
    public void showAddTag(ActionEvent event) {
        if (addTag == null) {
            addTag = Boolean.FALSE;
        }
        this.addTag = !this.addTag;
    }

    public String getTagsLabel() {
        return tagsLabel;
    }

    public void setTagsLabel(String tagsLabel) {
        this.tagsLabel = tagsLabel;
    }

    public Boolean getAddTag() {
        return addTag;
    }

    public void setAddTag(Boolean addTag) {
        this.addTag = addTag;
    }

}
