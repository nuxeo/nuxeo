/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.task.dashboard;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.5
 */
public abstract class AbstractDashBoardItemImpl implements DashBoardItem {

    private static final long serialVersionUID = 1L;

    protected static Log log = LogFactory.getLog(AbstractDashBoardItemImpl.class);

    protected Locale locale;

    protected String getI18nLabel(String label, Locale locale) {
        if (label == null) {
            label = "";
        }
        if (locale == null) {
            return label;
        }
        return I18NUtils.getMessageString("messages", label, null, locale);
    }

    public String getI18nTaskName() {
        if (locale == null) {
            return getName();
        }

        String labelKey = getName();
        if (needi18n()) {
            labelKey = "label.workflow.task." + labelKey;
        }
        return getI18nLabel(labelKey, locale);
    }

    public String getI18nDirective() {
        String directiveKey = getDirective();
        if (directiveKey == null) {
            directiveKey = getName();
        }
        if (locale == null) {
            return directiveKey;
        }

        String directiveLabel = getI18nLabel(directiveKey, locale);
        if (directiveKey != null && directiveKey.equals(directiveLabel)) {
            if (needi18n()) {
                directiveKey = "label.workflow.task." + directiveKey;
            }
            String newdirectiveLabel = getI18nLabel(directiveKey, locale);
            if (!directiveKey.equals(newdirectiveLabel)) {
                directiveLabel = newdirectiveLabel;
            }
        }
        return directiveLabel;
    }

    protected boolean isCreatedFromCreateTaskOperation() {
        return Boolean.parseBoolean(getTask().getVariable("createdFromCreateTaskOperation"));
    }

    protected boolean needi18n() {
        if (isCreatedFromCreateTaskOperation()) {
            return false;
        }
        return Boolean.parseBoolean(getTask().getVariable(Task.TaskVariableName.needi18n.name()));
    }

    public JSONObject asJSON() {

        boolean createdFromCreateTaskOperation = isCreatedFromCreateTaskOperation();

        JSONObject obj = new JSONObject();
        obj.put("id", getTask().getId());
        obj.put("docref", getDocument().getRef().toString());
        obj.put("name", getName());
        obj.put("taskName", getI18nTaskName());
        obj.put("directive", getI18nDirective());
        String comment = getComment();
        obj.put("comment", comment != null ? comment : "");
        Date dueDate = getDueDate();
        obj.put("dueDate", dueDate != null ? DateParser.formatW3CDateTime(dueDate) : "");
        obj.put("documentTitle", getDocument().getTitle());
        obj.put("documentLink", getDocumentLink(false));
        Date startDate = getStartDate();
        obj.put("startDate", startDate != null ? DateParser.formatW3CDateTime(startDate) : "");
        boolean expired = false;
        if (dueDate != null) {
            expired = dueDate.before(new Date());
        }
        obj.put("expired", expired);
        return obj;

    }

    public String getDocumentLink(boolean includeWorkflowTab) {
        DocumentModel doc = getDocument();
        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        if (documentViewCodecManager != null) {
            String viewId = getDefaultViewFor(doc);
            Map<String, String> parameters = new HashMap<String, String>();
            if (includeWorkflowTab) {
                parameters.put("tabId", "TAB_CONTENT_JBPM");
            }
            DocumentView docView = new DocumentViewImpl(new DocumentLocationImpl(doc), viewId, parameters);
            return documentViewCodecManager.getUrlFromDocumentView("docpath", docView, false, null);
        } else {
            return "";
        }
    }

    protected String getDefaultViewFor(DocumentModel doc) {
        TypeInfo type = doc.getAdapter(TypeInfo.class);
        if (type == null) {
            return null;
        }
        return type.getDefaultView();
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

}
