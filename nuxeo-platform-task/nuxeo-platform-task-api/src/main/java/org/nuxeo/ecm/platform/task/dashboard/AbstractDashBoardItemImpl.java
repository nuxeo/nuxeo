/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
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
        if (locale == null || !needi18n()) {
            return getName();
        }
        String labelKey = "label.workflow.task." + getName();
        return getI18nLabel(labelKey, locale);
    }

    public String getI18nDirective() {
        String directiveKey = getDirective();
        if (directiveKey == null) {
            directiveKey = getName();
        }
        if (locale == null || !needi18n()) {
            return directiveKey;
        }
        String directiveLabel = getI18nLabel(directiveKey, locale);
        if (directiveKey != null && directiveKey.equals(directiveLabel)) {
            directiveKey = "label.workflow.task." + directiveKey;
            String newdirectiveLabel = getI18nLabel(directiveKey, locale);
            if (!directiveKey.equals(newdirectiveLabel)) {
                directiveLabel = newdirectiveLabel;
            }
        }
        return directiveLabel;
    }

    protected boolean isCreatedFromCreateTaskOperation() throws ClientException {
        return Boolean.parseBoolean(getTask().getVariable(
                "createdFromCreateTaskOperation"));
    }

    protected boolean needi18n() {
        try {
            if (isCreatedFromCreateTaskOperation()) {
                return false;
            }
            return Boolean.parseBoolean(getTask().getVariable(
                    Task.TaskVariableName.needi18n.name()));
        } catch (Exception e) {
            log.error("Error while testing Task variables", e);
            return false;
        }
    }

    public JSONObject asJSON() throws ClientException {

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
        obj.put("dueDate",
                dueDate != null ? DateParser.formatW3CDateTime(dueDate) : "");
        obj.put("documentTitle", getDocument().getTitle());
        obj.put("documentLink",
                getDocumentLink(false));
        Date startDate = getStartDate();
        obj.put("startDate",
                startDate != null ? DateParser.formatW3CDateTime(startDate)
                        : "");
        boolean expired = false;
        if (dueDate != null) {
            expired = dueDate.before(new Date());
        }
        obj.put("expired", expired);
        return obj;

    }

    public String getDocumentLink(boolean includeWorkflowTab)
            throws ClientException {
        DocumentModel doc = getDocument();
        DocumentViewCodecManager documentViewCodecManager = Framework.getLocalService(DocumentViewCodecManager.class);
        if (documentViewCodecManager != null) {
            String viewId = getDefaultViewFor(doc);
            Map<String, String> parameters = new HashMap<String, String>();
            if (includeWorkflowTab) {
                parameters.put("tabId", "TAB_CONTENT_JBPM");
            }
            DocumentView docView = new DocumentViewImpl(
                    new DocumentLocationImpl(doc), viewId, parameters);
            return documentViewCodecManager.getUrlFromDocumentView("docpath",
                    docView, false, null);
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
