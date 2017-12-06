/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.restapi.server.jaxrs.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.AuditAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Enricher that add the latest log entries related to the document.
 *
 * @since 8.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AuditJsonEnricher extends AbstractJsonEnricher<DocumentModel>  {

    public static final String NAME = "audit";

    public AuditJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartArray();
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            DocumentModel searchDocument = wrapper.getSession().createDocumentModel("BasicAuditSearch");
            searchDocument.setPropertyValue("bas:eventIds", (Serializable) ctx.getParameters(AuditAdapter.EVENT_ID_PARAMETER_NAME));
            searchDocument.setPropertyValue("bas:eventCategories", (Serializable) ctx.getParameters(AuditAdapter.CATEGORY_PARAMETER_NAME));
            searchDocument.setPropertyValue("bas:principalNames", (Serializable) ctx.getParameters(AuditAdapter.PRINCIPAL_NAME_PARAMETER_NAME));
            searchDocument.setPropertyValue("bas:startDate", AuditAdapter.getCalendarParameter(ctx.getParameter(AuditAdapter.START_EVENT_DATE_PARAMETER_NAME)));
            searchDocument.setPropertyValue("bas:endDate", AuditAdapter.getCalendarParameter(ctx.getParameter(AuditAdapter.END_EVENT_DATE_PARAMETER_NAME)));

            PageProviderService ppService = Framework.getService(PageProviderService.class);
            PageProviderDefinition ppDefinition = ppService.getPageProviderDefinition(AuditAdapter.PAGE_PROVIDER_NAME);
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) wrapper.getSession());
            @SuppressWarnings("unchecked")
            PageProvider<LogEntry> pp = (PageProvider<LogEntry>) ppService.getPageProvider("", ppDefinition, searchDocument, null,
                    null, 0L, props, new Object[] {document});
            for (LogEntry e : pp.getCurrentPage()) {
                writeEntity(e, jg);
            }
        } finally {
            jg.writeEndArray();
        }
    }

}
