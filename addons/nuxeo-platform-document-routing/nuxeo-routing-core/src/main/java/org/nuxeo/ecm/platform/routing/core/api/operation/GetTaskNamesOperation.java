/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     guillaume
 */
package org.nuxeo.ecm.platform.routing.core.api.operation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Returns a list of current user open tasks where their translated name matches (partially or fully) the 'searchTerm'
 * parameter. This operation is invoked from a select2widget and the number of returned results is limited to 15.
 *
 * @since 5.8
 */
@Operation(id = GetTaskNamesOperation.ID, category = Constants.CAT_WORKFLOW, label = "Get Task Translated Names", description = "Returns a "
        + "list of current user open tasks where their translated name matches "
        + "(partially or fully ) the 'searchTerm' parameter. This operation is "
        + "invoked from a select2widget and the number of returned results is " + "limited to 15.", addToStudio = false)
public class GetTaskNamesOperation {

    public static final String ID = "Context.GetTaskNames";

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession session;

    @Param(name = "lang", required = false)
    protected String lang;

    @Param(name = "searchTerm", required = false)
    protected String searchTerm;

    @Param(name = "value", required = false)
    protected String value;

    @Param(name = "xpath", required = false)
    protected String xpath;

    /**
     * @since 8.4
     */
    @Param(name = "limit", required = false)
    protected int limit = -1;

    @OperationMethod
    public DocumentModelList run() {
        Locale locale = lang != null && !lang.isEmpty() ? new Locale(lang) : Locale.ENGLISH;
        if (value != null && !"".equals(value)) {
            return getAllUserOpenTask(session, locale, value, false);
        }
        return getAllUserOpenTask(session, locale, searchTerm, true);
    }

    /**
     * Returns all user tasks having their translated name matching ( partially or fully ) the given label.
     */
    protected DocumentModelList getAllUserOpenTask(CoreSession session, Locale locale, String searchTerm,
            boolean partialMatch) {
        DocumentModelList list = new DocumentModelListImpl();
        String query = "Select * from TaskDoc where ecm:mixinType IN ('RoutingTask') AND ecm:isVersion = 0 AND ecm:currentLifeCycleState = 'opened'";
        Map<String, DocumentModel> results = new HashMap<String, DocumentModel>();
        DocumentModelList docs = session.query(query);
        int i = 0;
        for (DocumentModel doc : docs) {
            String taskName = (String) doc.getPropertyValue("nt:name");
            String taskLabel = getI18nLabel(taskName, locale);
            if (partialMatch) {
                // a translaedLabel == "" corresponds to the list of all
                // tasks
                if (searchTerm == null || "".equals(searchTerm)) {
                    doc.setPropertyValue("dc:title",
                            "[" + getWorkflowTranslatedTitle(doc, locale) + "]" + " " + taskLabel);
                    results.put(taskName, doc);
                    i++;
                } else {
                    // add doc to result set only if the translated label
                    // starts with the 'searchTerm'
                    if (taskLabel.startsWith(searchTerm)) {
                        doc.setPropertyValue("dc:title",
                                "[" + getWorkflowTranslatedTitle(doc, locale) + "]" + " " + taskLabel);
                        results.put(taskName, doc);
                        i++;
                    }
                }
            }
            if (!partialMatch && searchTerm.equals(taskName)) {
                doc.setPropertyValue("dc:title", "[" + getWorkflowTranslatedTitle(doc, locale) + "]" + " " + taskLabel);
                results.put(taskName, doc);
                i++;
                break;
            }
            if (limit > 0 && i > limit) {
                break;
            }
        }
        list.addAll(results.values());
        return list;
    }

    protected String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        return I18NUtils.getMessageString("messages", label, null, locale);
    }

    protected String getWorkflowTranslatedTitle(DocumentModel taskDoc, Locale locale) throws PropertyException {
        String workflowId = (String) taskDoc.getPropertyValue("nt:processId");
        DocumentModel workflowDoc = session.getDocument(new IdRef(workflowId));
        return getI18nLabel(workflowDoc.getTitle(), locale);
    }
}
