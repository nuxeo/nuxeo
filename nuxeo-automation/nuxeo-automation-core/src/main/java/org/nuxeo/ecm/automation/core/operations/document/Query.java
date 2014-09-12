/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated Since 5.9.6, document query operation logic has been moved.
 * This class is not used/registered anymore into the platform as Automation
 * Operation. Replaced by {@link org.nuxeo.ecm.automation.core.operations
 * .document.DocumentQuery}.
 */
@Deprecated
@Operation(id = Query.ID, category = Constants.CAT_FETCH, label = "Query",
        description = "Perform a query on the repository. The query result " +
                "will become the input for the next operation.",
        addToStudio = true, deprecatedSince = "5.9.6")
public class Query {

    public static final String ID = "Document.Query";

    @Context
    protected CoreSession session;

    @Param(name = "query")
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION,
            values = {
                    NXQL.NXQL, "CMISQL" })
    protected String lang = NXQL.NXQL;

    @OperationMethod
    public DocumentModelList run() throws Exception {
        return session.query(query, lang, null, 0, 0, false);
    }

}
