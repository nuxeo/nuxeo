/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql;

import java.io.Reader;
import java.io.StringReader;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.parser.Scanner;
import org.nuxeo.ecm.core.query.sql.parser.parser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class SQLQueryParser {

    // Utility class
    private SQLQueryParser() {
    }

    public static SQLQuery parse(Reader reader) throws QueryParseException {
        try {
            Scanner scanner = new Scanner(reader);
            parser parser = new parser(scanner);
            return (SQLQuery) parser.parse().value;
        } catch (QueryParseException e) {
            throw e;
        } catch (Exception e) { // stupid CUPS API throws Exception
            throw new QueryParseException(ExceptionUtils.runtimeException(e));
        }
    }

    public static SQLQuery parse(String string) throws QueryParseException {
        try {
            SQLQuery query = parse(new StringReader(string));
            query.setQueryString(string);
            return query;
        } catch (QueryParseException e) {
            throw new QueryParseException(e.getMessage() + " in query: "
                    + string, e);
        }
    }

    /**
     * Returns the string literal in a form ready to embed in an NXQL statement.
     *
     * @deprecated since 5.7, 5.6.0-HF08 use {{@link NXQL#escapeString} instead
     */
    @Deprecated
    public static String prepareStringLiteral(String s) {
        return NXQL.escapeString(s);
    }

}
