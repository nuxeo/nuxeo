/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ecm.platform.tag.web;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jboss.el.lang.FunctionMapperImpl;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.el.EL;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.4-JSF2-SNAPSHOT
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
    public String resolveDocumentTags() throws ClientException {
        if (currentDocumentTags == null || currentDocumentTags.isEmpty()) {
            return "[]";
        } else {
            JSONArray result = new JSONArray();
            for (Tag tag : currentDocumentTags) {
                JSONObject obj = new JSONObject();
                obj.element(Select2Common.ID, tag.getLabel());
                obj.element(Select2Common.LABEL, tag.getLabel());
                result.add(obj);
            }
            return result.toString();
        }
    }

    public String resolveTags(final List<String> list) {
       return Select2Common.resolveDefaultEntries(list);
    }

    @Factory(value = "documentTagIds", scope = EVENT)
    public List<String> getDocumentTagStrings() throws ClientException {
        if (currentDocumentTags == null || currentDocumentTags.isEmpty()) {
            return null;
        } else {
            List<String> result = new ArrayList<String>();
            for (Tag tag : currentDocumentTags) {
                result.add(tag.getLabel());
            }
            return result;
        }
    }

    /**
     * Performs the tagging on the current document.
     */
    public String addTagging() throws ClientException {
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
                    tagService.tag(documentManager, liveDocument.getId(),
                            label, null);
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
        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get(messageKey), label);
        reset();
        return null;
    }

    public String removeTagging() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String docId = currentDocument.getId();

        TagService tagService = getTagService();
        tagService.untag(documentManager, docId, label, null);

        if (currentDocument.isVersion()) {
            DocumentModel liveDocument = documentManager.getSourceDocument(currentDocument.getRef());
            if (!liveDocument.isCheckedOut()) {
                tagService.untag(documentManager, liveDocument.getId(), label,
                        null);
            }
        } else if (!currentDocument.isCheckedOut()) {
            DocumentRef ref = documentManager.getBaseVersion(currentDocument.getRef());
            if (ref instanceof IdRef) {
                tagService.untag(documentManager, ref.toString(), label, null);
            }
        }
        // force invalidation
        Contexts.getEventContext().remove("currentDocumentTags");
        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("message.remove.tagging"), label);
        reset();
        return null;
    }

    protected void reset() {
        label = null;
    }

    protected TagService getTagService() throws ClientException {
        TagService tagService;
        try {
            tagService = Framework.getService(TagService.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        if (tagService == null) {
            return null;
        }
        return tagService.isEnabled() ? tagService : null;
    }


    public String encodePrameters(final Widget widget) {
        return encodeCommonPrameters(widget).toString();
    }

    public String encodeParametersForCurrentDocument(final Widget widget) {
        JSONObject obj = encodeCommonPrameters(widget);
        obj.put("onAddEntryHandler", "addTagHandler");
        obj.put("onRemoveEntryHandler", "removeTagHandler");
        obj.put("containerCssClass", "s2tagContainerCssClass");
        obj.put("dropdownCssClass", "s2tagDropdownCssClass");
        obj.put("createSearchChoice", "createNewTag");
        if (!widget.getProperties().containsKey("canSelectNewTag")
                && Boolean.getBoolean((String) widget.getProperties().get(
                        "canSelectNewTag"))) {
            obj.put("createSearchChoice", "createNewTag");
        }
        return obj.toString();
    }

    protected JSONObject encodeCommonPrameters(final Widget widget) {
        JSONObject obj = new JSONObject();
        Map<String, Serializable> widgetProperties = widget.getProperties();
        obj.put("multiple", "true");
        obj.put(Select2Common.MIN_CHARS, "1");
        obj.put(Select2Common.READ_ONLY_PARAM, "false");
        if (widgetProperties.containsKey("canSelectNewTag")
                && Boolean.getBoolean((String) widgetProperties.get(
                        "canSelectNewTag"))) {
            obj.put("createSearchChoice", "createNewTag");
        }
        obj.put(Select2Common.OPERATION_ID, "Tag.Suggestion");
        obj.put(Select2Common.WIDTH, "300px");
        if (widgetProperties.containsKey("placeholder")) {
            ELContext elContext = EL.createELContext(
                    SeamActionContext.EL_RESOLVER, new FunctionMapperImpl());
            String placeholder = (String) SeamActionContext.EXPRESSION_FACTORY.createValueExpression(
                    elContext,
                    (String) widgetProperties.get("placeholder"),
                    String.class).getValue(elContext);
            obj.put(Select2Common.PLACEHOLDER, placeholder);
        }
        obj.put(Select2Common.SUGGESTION_FORMATTER, "formatSuggestedTags");
        JSONArray tokenSeparator = new JSONArray();
        tokenSeparator.add(",");
        tokenSeparator.add(" ");
        obj.put("tokenSeparators", tokenSeparator);
        return obj;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}