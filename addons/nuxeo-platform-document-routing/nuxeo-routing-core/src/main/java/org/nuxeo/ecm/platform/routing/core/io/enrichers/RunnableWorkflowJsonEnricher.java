/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.routing.core.io.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enricher that add the runnable workflow model on a document for the current user.
 *
 * @since 8.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class RunnableWorkflowJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "runnableWorkflows";

    @Inject
    ActionManager actionManager;

    public RunnableWorkflowJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeFieldName(NAME);
        jg.writeStartArray();
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            DocumentRoutingService documentRoutingService = Framework.getService(DocumentRoutingService.class);
            List<DocumentModel> routeModels = documentRoutingService.searchRouteModels(wrapper.getSession(), "");

            ActionContext actionContext = new ELActionContext();
            actionContext.setCurrentDocument(document);
            actionContext.setDocumentManager(wrapper.getSession());
            actionContext.setCurrentPrincipal(wrapper.getSession().getPrincipal());

            for (Iterator<DocumentModel> it = routeModels.iterator(); it.hasNext();) {
                DocumentModel route = it.next();
                Object graphRouteObj = route.getAdapter(GraphRoute.class);
                if (graphRouteObj instanceof GraphRoute) {
                    String filter = ((GraphRoute) graphRouteObj).getAvailabilityFilter();
                    if (!StringUtils.isBlank(filter)) {
                        if (!actionManager.checkFilter(filter, actionContext)) {
                            it.remove();
                        }
                    }
                } else {
                    // old workflow document => ignore
                    it.remove();
                }
            }
            for (DocumentModel documentRoute : routeModels) {
                writeEntity(documentRoute.getAdapter(DocumentRoute.class), jg);
            }
        } finally {
            jg.writeEndArray();
        }
    }

}
