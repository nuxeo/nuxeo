/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
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
 * @since 5.9.6
 * Document query operation to perform queries on the repository.
 */
@Operation(id = DocumentQuery.ID, category = Constants.CAT_FETCH,
        label = "Query", description = "Perform a query on the repository. " +
        "The query document list result will become the input for the next " +
        "operation.", since = "5.9.6", addToStudio = true)
public class DocumentQuery {

    public static final String ID = "Document.Query";

    @Context
    protected CoreSession session;

    @Param(name = "query", required = true, description = "The query to " +
            "perform.")
    protected String query;

    @Param(name = "language", required = false, description = "The query " +
            "language.", widget = Constants.W_OPTION,
            values = { NXQL.NXQL, "CMISQL", "ESQL" })
    protected String lang = NXQL.NXQL;


    @OperationMethod
    public DocumentModelList run() throws Exception {
        return session.query(query, lang, null, 0, 0, false);
    }

}
