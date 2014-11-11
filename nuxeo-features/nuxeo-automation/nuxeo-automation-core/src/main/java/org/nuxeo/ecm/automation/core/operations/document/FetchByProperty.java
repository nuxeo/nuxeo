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

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.query.sql.NXQL;

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
        if (values.isEmpty()) {
            return new DocumentModelListImpl();
        }
        if (StringUtils.isBlank(query)) {
            query = null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM Document WHERE ");
        sb.append(property);
        if (values.size() == 1) {
            sb.append(" = ");
            sb.append(NXQL.escapeString(values.get(0)));
        } else {
            sb.append(" IN (");
            for (Iterator<String> it = values.iterator(); it.hasNext();) {
                sb.append(NXQL.escapeString(it.next()));
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
        if (query != null) {
            sb.append(" AND (");
            sb.append(query);
            sb.append(")");
        }
        String q = sb.toString();
        return session.query(q);
    }

}
