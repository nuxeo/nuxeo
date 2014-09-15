/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter that returns the log entries of the pointed resource.
 *
 * @since 5.7.3
 */
@WebAdapter(name = AuditAdapter.NAME, type = "AuditService")
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class AuditAdapter extends PaginableAdapter<LogEntry> {

    public static final String NAME = "audit";

    public static final String PAGE_PROVIDER_NAME = "DOCUMENT_HISTORY_PROVIDER";

    public static final String EVENT_ID_PARAMETER_NAME = "eventId";

    public static final String CATEGORY_PARAMETER_NAME = "category";

    public static final String PRINCIPAL_NAME_PARAMETER_NAME = "principalName";

    public static final String START_EVENT_DATE_PARAMETER_NAME = "startEventDate";

    public static final String END_EVENT_DATE_PARAMETER_NAME = "endEventDate";

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { getTarget().getAdapter(DocumentModel.class) };
    }

    @Override
    protected DocumentModel getSearchDocument() throws ClientException {
        HttpServletRequest request = ctx.getRequest();
        CoreSession session = ctx.getCoreSession();

        DocumentModel searchDocument = session.createDocumentModel("BasicAuditSearch");
        searchDocument.setPropertyValue("bas:eventIds",
                request.getParameterValues(EVENT_ID_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:eventCategories",
                request.getParameterValues(CATEGORY_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:principalNames",
                request.getParameterValues(PRINCIPAL_NAME_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:startDate",
                getCalendarParameter(request, START_EVENT_DATE_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:endDate",
                getCalendarParameter(request, END_EVENT_DATE_PARAMETER_NAME));

        return searchDocument;
    }

    protected Calendar getCalendarParameter(HttpServletRequest request,
            String paramName) {
        String param = request.getParameter(paramName);
        if (param != null) {
            DateTime date = ISODateTimeFormat.date().parseDateTime(param);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date.toDate());
                return cal;
            }
        }
        return null;
    }
}
