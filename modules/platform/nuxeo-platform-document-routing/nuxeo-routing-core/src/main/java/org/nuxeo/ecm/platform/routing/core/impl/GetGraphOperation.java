/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.Locale;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.jsongraph.JsonGraphRoute;

/**
 * Returns a json representation of the graph route
 *
 * @since 5.6
 */
@Operation(id = GetGraphOperation.ID, category = DocumentRoutingConstants.OPERATION_CATEGORY_ROUTING_NAME, label = "Get graph", description = "get graph nodes.", addToStudio = false)
public class GetGraphOperation {
    public final static String ID = "Document.Routing.GetGraph";

    @Context
    protected OperationContext context;

    @Param(name = "routeDocId", required = true)
    protected String routeDocId;

    /***
     * @since 5.7
     */
    @Param(name = "language", required = false)
    protected String language;

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run() {
        Locale locale = language != null && !language.isEmpty() ? new Locale(language) : Locale.ENGLISH;
        JsonGraphRoute unrestrictedRunner = new JsonGraphRoute(session, routeDocId, locale);
        String json = unrestrictedRunner.getJSON();
        return Blobs.createJSONBlob(json);
    }

}
