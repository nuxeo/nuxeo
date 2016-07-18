/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.platform.tag.web;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Helper component for tagging widget relying on select2.
 *
 * @since 6.0
 */
@Name("tagSelect2Support")
@Scope(EVENT)
public class TagSelect2Support {

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, value = "currentDocumentTags")
    protected List<Tag> currentDocumentTags;

    protected String label;

    @Factory(value = "resolveDocumentTags", scope = EVENT)
    public String resolveDocumentTags() {
        if (currentDocumentTags == null || currentDocumentTags.isEmpty()) {
            return "[]";
        } else {
            String docId = navigationContext.getCurrentDocument().getId();
            TagService tagService = getTagService();
            JSONArray result = new JSONArray();
            for (Tag tag : currentDocumentTags) {
                JSONObject obj = new JSONObject();
                obj.element(Select2Common.ID, tag.getLabel());
                obj.element(Select2Common.LABEL, tag.getLabel());
                obj.element(Select2Common.LOCKED, !tagService.canUntag(documentManager, docId, tag.getLabel()));
                result.add(obj);
            }
            return result.toString();
        }
    }

    public String resolveTags(final List<String> list) {
        return Select2Common.resolveDefaultEntries(list);
    }

    public String resolveTags(final String[] array) {
        if (array == null || array.length == 0) {
            return Select2Common.resolveDefaultEntries(null);
        }
        return Select2Common.resolveDefaultEntries(Arrays.asList(array));
    }

    @Factory(value = "documentTagIds", scope = EVENT)
    public List<String> getDocumentTagStrings() {
        if (currentDocumentTags == null || currentDocumentTags.isEmpty()) {
            return null;
        } else {
            return currentDocumentTags.stream().map(Tag::getLabel).collect(Collectors.toList());
        }
    }

    /**
     * Performs the tagging on the current document.
     */
    public String addTagging() {
        String messageKey;
        if (StringUtils.isBlank(label)) {
            messageKey = "message.add.new.tagging.not.empty";
        } else {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            String docId = currentDocument.getId();

            TagService tagService = getTagService();
            tagService.tag(documentManager, docId, label, null);
            if (currentDocument.isVersion()) {
                DocumentModel liveDocument = documentManager.getSourceDocument(currentDocument.getRef());
                if (!liveDocument.isCheckedOut()) {
                    tagService.tag(documentManager, liveDocument.getId(), label, null);
                }
            } else if (!currentDocument.isCheckedOut()) {
                DocumentRef ref = documentManager.getBaseVersion(currentDocument.getRef());
                if (ref instanceof IdRef) {
                    tagService.tag(documentManager, ref.toString(), label, null);
                }
            }
            messageKey = "message.add.new.tagging";
            // force invalidation
            Contexts.getEventContext().remove("resolveDocumentTags");
        }
        facesMessages.add(StatusMessage.Severity.INFO, messages.get(messageKey), label);
        reset();
        return null;
    }

    public String removeTagging() {
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
        // force invalidation
        Contexts.getEventContext().remove("currentDocumentTags");
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("message.remove.tagging"), label);
        reset();
        return null;
    }

    protected void reset() {
        label = null;
    }

    protected TagService getTagService() {
        TagService tagService = Framework.getService(TagService.class);
        return tagService.isEnabled() ? tagService : null;
    }

    public String encodeParameters(final Map<String, Serializable> widgetProperties) {
        return encodeCommonParameters(widgetProperties).toString();
    }

    public String encodeParametersForCurrentDocument(final Map<String, Serializable> widgetProperties) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("onAddEntryHandler", "addTagHandler");
        parameters.put("onRemoveEntryHandler", "removeTagHandler");
        parameters.put("containerCssClass", "s2tagContainerCssClass");
        parameters.put("dropdownCssClass", "s2tagDropdownCssClass");
        parameters.put("createSearchChoice", "createNewTag");
        if (widgetProperties.containsKey("canSelectNewTag")
                && !Boolean.parseBoolean((String) widgetProperties.get("canSelectNewTag"))) {
            parameters.remove("createSearchChoice");
        }
        return encodeCommonParameters(widgetProperties, parameters).toString();
    }

    protected JSONObject encodeCommonParameters(final Map<String, Serializable> widgetProperties) {
        return encodeCommonParameters(widgetProperties, null);
    }

    protected JSONObject encodeCommonParameters(final Map<String, Serializable> widgetProperties,
            final Map<String, String> additionalParameters) {
        JSONObject obj = new JSONObject();
        obj.put("multiple", "true");
        obj.put(Select2Common.MIN_CHARS, "1");
        obj.put(Select2Common.READ_ONLY_PARAM, "false");
        if (widgetProperties.containsKey("canSelectNewTag")
                && Boolean.parseBoolean((String) widgetProperties.get("canSelectNewTag"))) {
            obj.put("createSearchChoice", "createNewTag");
        }
        obj.put(Select2Common.OPERATION_ID, "Tag.Suggestion");
        obj.put(Select2Common.WIDTH, "300px");
        obj.put(Select2Common.SELECTION_FORMATTER, "formatSelectedTags");
        obj.put(Select2Common.SUGGESTION_FORMATTER, "formatSuggestedTags");
        JSONArray tokenSeparator = new JSONArray();
        tokenSeparator.add(",");
        tokenSeparator.add(" ");
        obj.put("tokenSeparators", tokenSeparator);
        if (additionalParameters != null) {
            for (Entry<String, String> entry : additionalParameters.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
        }
        for (Entry<String, Serializable> entry : widgetProperties.entrySet()) {
            obj.put(entry.getKey(), entry.getValue().toString());
        }
        return obj;
    }

    /**
     * @since 7.1
     */
    public void listDocumentsForTag() {
        final TagActionsBean tagActionsBean = (TagActionsBean) Component.getInstance(TagActionsBean.class);
        tagActionsBean.setListLabel(label);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
