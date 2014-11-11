/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.automation.jbpm;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.platform.jbpm.dashboard.DocumentProcessItem;
import org.nuxeo.ecm.platform.jbpm.providers.UserProcessPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation to retrieve the current user's processes.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
@Operation(id = UserProcessPageProviderOperation.ID, category = Constants.CAT_SERVICES, label = "UserProcessPageProvider", description = "Returns the current user's processes.")
public class UserProcessPageProviderOperation extends AbstractWorkflowOperation {

    public static final String ID = "Workflow.UserProcessPageProvider";

    public static final String USER_PROCESSES_PAGE_PROVIDER = "user_processes";

    @Param(name = "language", required = false)
    protected String language;

    @Param(name = "page", required = false)
    protected Integer page;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    @Context
    protected CoreSession session;

    @Context
    protected DocumentViewCodecManager documentViewCodecManager;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public Blob run() throws Exception {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(UserProcessPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        PageProviderService pps = Framework.getLocalService(PageProviderService.class);

        Long targetPage = null;
        if (page != null) {
            targetPage = Long.valueOf(page.longValue());
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = Long.valueOf(pageSize.longValue());
        }
        PageProvider<DocumentProcessItem> pageProvider = (PageProvider<DocumentProcessItem>) pps.getPageProvider(
                USER_PROCESSES_PAGE_PROVIDER, null, targetPageSize, targetPage,
                props);

        Locale locale = language != null && !language.isEmpty() ? new Locale(
                language) : Locale.ENGLISH;

        JSONArray processes = new JSONArray();
        for (DocumentProcessItem process : pageProvider.getCurrentPage()) {
            JSONObject obj = new JSONObject();
            obj.put("processInstanceName", getI18nProcessInstanceName(
                    process.getProcessInstanceName(), locale));
            obj.put("documentTitle", process.getDocumentModel().getTitle());
            obj.put("documentLink", getDocumentLink(documentViewCodecManager,
                    process.getDocumentModel(), true));
            Date startDate = process.getProcessInstanceStartDate();
            obj.put("startDate",
                    startDate != null ? DateParser.formatW3CDateTime(startDate)
                            : "");
            process.getProcessInstanceName();
            processes.add(obj);
        }

        JSONObject json = new JSONObject();
        json.put("isPaginable", Boolean.TRUE);
        json.put("totalSize", Long.valueOf(pageProvider.getResultsCount()));
        json.put("pageIndex", Long.valueOf(pageProvider.getCurrentPageIndex()));
        json.put("pageSize", Long.valueOf(pageProvider.getPageSize()));
        json.put("pageCount", Long.valueOf(pageProvider.getNumberOfPages()));

        json.put("entries", processes);
        return new InputStreamBlob(new ByteArrayInputStream(
                json.toString().getBytes("UTF-8")), "application/json");
    }

    protected String getI18nProcessInstanceName(String processInstanceName,
            Locale locale) {
        String labelKey = "document_" + processInstanceName;
        return getI18nLabel(labelKey, locale);
    }

}
