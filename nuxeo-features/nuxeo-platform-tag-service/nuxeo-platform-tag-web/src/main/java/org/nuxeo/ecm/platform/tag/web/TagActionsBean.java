/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Radu Darlea
 *     Bogdan Tatar
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag.web;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * This Seam bean provides support for tagging related actions which can be made on the current document.
 */
@Name("tagActions")
@Scope(CONVERSATION)
public class TagActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TagActionsBean.class);

    public static final String TAG_SEARCH_RESULT_PAGE = "tag_search_results";

    public static final String SELECTION_EDITED = "selectionEdited";

    public static final String DOCUMENTS_IMPORTED = "documentImported";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected String listLabel;

    // protected LRUCachingMap<String, Boolean> tagModifyCheckCache = new
    // LRUCachingMap<String, Boolean>(
    // 1);

    /**
     * Keeps the tagging information that will be performed on the current document document.
     */
    private String tagLabel;

    /**
     * Controls the presence of the tagging text field in UI.
     */
    private boolean addTag;

    @RequestParameter
    protected Boolean canSelectNewTag;

    @Factory(value = "tagServiceEnabled", scope = APPLICATION)
    public boolean isTagServiceEnabled() {
        return getTagService() != null;
    }

    protected TagService getTagService() {
        TagService tagService = Framework.getService(TagService.class);
        return tagService.isEnabled() ? tagService : null;
    }

    /**
     * Returns the list with distinct public tags (or owned by user) that are applied on the current document.
     */
    @Factory(value = "currentDocumentTags", scope = EVENT)
    public List<Tag> getDocumentTags() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return new ArrayList<Tag>(0);
        } else {
            String docId = currentDocument.getId();
            List<Tag> tags = getTagService().getDocumentTags(documentManager, docId, null);
            Collections.sort(tags, Tag.LABEL_COMPARATOR);
            return tags;
        }
    }

    /**
     * Performs the tagging on the current document.
     */
    public String addTagging() {
        tagLabel = cleanLabel(tagLabel);
        String messageKey;
        if (StringUtils.isBlank(tagLabel)) {
            messageKey = "message.add.new.tagging.not.empty";
        } else {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            String docId = currentDocument.getId();

            TagService tagService = getTagService();
            tagService.tag(documentManager, docId, tagLabel, null);
            if (currentDocument.isVersion()) {
                DocumentModel liveDocument = documentManager.getSourceDocument(currentDocument.getRef());
                if (!liveDocument.isCheckedOut()) {
                    tagService.tag(documentManager, liveDocument.getId(), tagLabel, null);
                }
            } else if (!currentDocument.isCheckedOut()) {
                DocumentRef ref = documentManager.getBaseVersion(currentDocument.getRef());
                if (ref instanceof IdRef) {
                    tagService.tag(documentManager, ref.toString(), tagLabel, null);
                }
            }
            messageKey = "message.add.new.tagging";
            // force invalidation
            Contexts.getEventContext().remove("currentDocumentTags");
        }
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(messageKey), tagLabel);
        reset();
        return null;
    }

    /**
     * Removes a tagging from the current document.
     */
    public String removeTagging(String label) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String docId = currentDocument.getId();

        TagService tagService = getTagService();
        tagService.untag(documentManager, docId, label, null);

        if (currentDocument.isVersion()) {
            DocumentModel liveDocument = documentManager.getSourceDocument(currentDocument.getRef());
            if (!liveDocument.isCheckedOut()) {
                tagService.untag(documentManager, liveDocument.getId(), label, null);
            }
        } else if (!currentDocument.isCheckedOut()) {
            DocumentRef ref = documentManager.getBaseVersion(currentDocument.getRef());
            if (ref instanceof IdRef) {
                tagService.untag(documentManager, ref.toString(), label, null);
            }
        }

        reset();
        // force invalidation
        Contexts.getEventContext().remove("currentDocumentTags");
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("message.remove.tagging"),
                label);
        return null;
    }

    /**
     * Returns tag cloud info for the whole repository. For performance reasons, the security on underlying documents is
     * not tested.
     */
    @Factory(value = "tagCloudOnAllDocuments", scope = EVENT)
    public List<Tag> getPopularCloudOnAllDocuments() {
        List<Tag> cloud = getTagService().getTagCloud(documentManager, null, null, Boolean.TRUE); // logarithmic 0-100
                                                                                                  // normalization
        // change weight to a font size
        double min = 100;
        double max = 200;
        for (Tag tag : cloud) {
            tag.setWeight((long) (min + tag.getWeight() * (max - min) / 100));
        }
        Collections.sort(cloud, Tag.LABEL_COMPARATOR);
        // Collections.sort(cloud, Tag.WEIGHT_COMPARATOR);
        return cloud;
    }

    public String listDocumentsForTag(String listLabel) {
        this.listLabel = listLabel;
        return TAG_SEARCH_RESULT_PAGE;
    }

    @Factory(value = "taggedDocuments", scope = EVENT)
    public DocumentModelList getChildrenSelectModel() {
        if (StringUtils.isBlank(listLabel)) {
            return new DocumentModelListImpl(0);
        } else {
            List<String> ids = getTagService().getTagDocumentIds(documentManager, listLabel, null);
            DocumentModelList docs = new DocumentModelListImpl(ids.size());
            DocumentModel doc = null;
            for (String id : ids) {
                try {
                    doc = documentManager.getDocument(new IdRef(id));
                } catch (DocumentNotFoundException e) {
                    log.error(e.getMessage(), e);
                } catch (DocumentSecurityException e) {
                    // user don't have access to the document
                    doc = null;
                }
                if (doc != null) {
                    docs.add(doc);
                    doc = null;
                }
            }
            return docs;
        }
    }

    public String getListLabel() {
        return listLabel;
    }

    public void setListLabel(String listLabel) {
        this.listLabel = listLabel;
    }

    /**
     * Returns <b>true</b> if the current logged user has permission to modify a tag that is applied on the current
     * document.
     */
    public boolean canModifyTag(Tag tag) {
        return tag != null;
    }

    /**
     * Resets the fields that are used for managing actions related to tagging.
     */
    public void reset() {
        tagLabel = null;
    }

    /**
     * Used to decide whether the tagging UI field is shown or not.
     */
    public void showAddTag(ActionEvent event) {
        this.addTag = !this.addTag;
    }

    public String getTagLabel() {
        return tagLabel;
    }

    /**
     * @since 7.1
     */
    public void setTagLabel(final String tagLabel) {
        this.tagLabel = tagLabel;
    }

    public boolean getAddTag() {
        return addTag;
    }

    public void setAddTag(boolean addTag) {
        this.addTag = addTag;
    }

    public List<Tag> getSuggestions(Object input) {
        String label = (String) input;
        List<Tag> tags = getTagService().getSuggestions(documentManager, label, null);
        Collections.sort(tags, Tag.LABEL_COMPARATOR);
        if (tags.size() > 10) {
            tags = tags.subList(0, 10);
        }

        // add the typed tag as first suggestion if we can add new tag
        label = cleanLabel(label);
        if (Boolean.TRUE.equals(canSelectNewTag) && !tags.contains(new Tag(label, 0))) {
            tags.add(0, new Tag(label, -1));
        }

        return tags;
    }

    protected static String cleanLabel(String label) {
        label = label.toLowerCase(); // lowercase
        label = label.replace(" ", ""); // no spaces
        label = label.replace("\\", ""); // dubious char
        label = label.replace("'", ""); // dubious char
        label = label.replace("%", ""); // dubious char
        return label;
    }

    @SuppressWarnings("unchecked")
    @Observer({ SELECTION_EDITED, DOCUMENTS_IMPORTED })
    public void addTagsOnEvent(List<DocumentModel> documents, DocumentModel docModel) {
        List<String> tags = (List<String>) docModel.getContextData(ScopeType.REQUEST, "bulk_tags");
        if (tags != null && !tags.isEmpty()) {
            TagService tagService = Framework.getLocalService(TagService.class);
            String username = documentManager.getPrincipal().getName();
            for (DocumentModel doc : documents) {
                for (String tag : tags) {
                    tagService.tag(documentManager, doc.getId(), tag, username);
                }
            }
        }
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        addTag = false;
    }

}
