/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        return Blobs.createBlob(json.toString(), "application/json");
    }

}
