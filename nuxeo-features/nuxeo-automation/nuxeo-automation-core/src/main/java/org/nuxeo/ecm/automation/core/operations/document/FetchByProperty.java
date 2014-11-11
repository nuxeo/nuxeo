/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FetchByProperty.ID, category = Constants.CAT_FETCH, label = "Fetch By Property", description = "For each specified string property value, fetch all documents that match the property and the optional where clause. Matching documents are collected into a list and the returned to the next operation. The operation has no input.")
public class FetchByProperty {

    public static final String ID = "Document.FetchByProperty";

    @Context
    protected CoreSession session;

    @Param(name = "property", required = true)
    protected String property;

    @Param(name = "values", required = true)
    protected StringList values;

    @Param(name = "query", required = false)
    protected String query;

    @OperationMethod
    public DocumentModelList run() throws Exception {
        // TODO use SQLQueryParser.prepareStringLiteral to escape the string
        // values.
        if (query != null && query.trim().length() == 0) {
            query = null;
        }
        String queryWhereClause = "SELECT * FROM Document WHERE %s=\"%s\" AND (%s)";
        String queryNoWhereClause = "SELECT * FROM Document WHERE %s=\"%s\"";
        DocumentModelList result = new DocumentModelListImpl();
        for (String value : values) {
            String q = query != null ? String.format(queryWhereClause,
                    property, value, query) : String.format(queryNoWhereClause,
                    property, value);
            DocumentModelList docs = session.query(q);
            result.addAll(docs);
        }
        return result;
    }
}
