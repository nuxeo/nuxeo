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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.utils.DateParser;
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
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class AuditAdapter extends PaginableAdapter<LogEntry> {

    private static Log log = LogFactory.getLog(AuditAdapter.class);

    public static final String NAME = "audit";

    public static final String PAGE_PROVIDER_NAME = "DOCUMENT_HISTORY_PROVIDER";

    public static final String EVENT_ID_PARAMETER_NAME = "eventId";

    public static final String CATEGORY_PARAMETER_NAME = "category";

    public static final String PRINCIPAL_NAME_PARAMETER_NAME = "principalName";

    public static final String START_EVENT_DATE_PARAMETER_NAME = "startEventDate";

    public static final String END_EVENT_DATE_PARAMETER_NAME = "endEventDate";

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        return ppService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { getTarget().getAdapter(DocumentModel.class) };
    }

    @Override
    protected DocumentModel getSearchDocument() {
        HttpServletRequest request = ctx.getRequest();
        CoreSession session = ctx.getCoreSession();

        DocumentModel searchDocument = session.createDocumentModel("BasicAuditSearch");
        searchDocument.setPropertyValue("bas:eventIds", request.getParameterValues(EVENT_ID_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:eventCategories", request.getParameterValues(CATEGORY_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:principalNames", request.getParameterValues(PRINCIPAL_NAME_PARAMETER_NAME));
        searchDocument.setPropertyValue("bas:startDate", getCalendarParameter(request.getParameter(START_EVENT_DATE_PARAMETER_NAME)));
        searchDocument.setPropertyValue("bas:endDate", getCalendarParameter(request.getParameter(END_EVENT_DATE_PARAMETER_NAME)));

        return searchDocument;
    }

    public static Calendar getCalendarParameter(String param) {
        if (param != null) {
            Calendar cal = Calendar.getInstance();
            try {
                Date date = DateParser.parseW3CDateTime(param);
                if (date != null) {
                    cal.setTime(date);
                    return cal;
                }
            } catch (IllegalArgumentException e) {
                // Backward compat
                log.warn("Date should have 'YYYY-MM-DDThh:mm:ss.sTZD' format, trying to parse 'yyyy-MM-dd' format");
                DateTime date = ISODateTimeFormat.date().parseDateTime(param);
                if (date != null) {
                    cal.setTime(date.toDate());
                    return cal;
                }
            }
        }
        return null;
    }
}
