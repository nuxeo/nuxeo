/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query.sql;

import java.io.Reader;
import java.io.StringReader;

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

    public static SQLQuery parse(Reader reader) {
        try {
            Scanner scanner = new Scanner(reader);
            parser parser = new parser(scanner);
            return (SQLQuery) parser.parse().value;
        } catch (Exception e) {
            throw new QueryParseException(e);
        }
    }

    public static SQLQuery parse(String string) {
        try {
            return parse(new StringReader(string));
        } catch (QueryParseException e) {
            throw new QueryParseException(e.getMessage() + " in query: "
                    + string, e);
        }
    }

    /**
     * Return the string literal in a form ready to embed in an NXQL statement.
     *
     * @param s
     * @return
     */
    public static String prepareStringLiteral(String s) {
        return "'" + s.replaceAll("'", "\\\\'") + "'";
    }

}
