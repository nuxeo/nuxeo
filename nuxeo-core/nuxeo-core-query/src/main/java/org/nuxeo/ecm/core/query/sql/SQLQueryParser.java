/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
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
        SQLQuery query = parse(new StringReader(string));
        query.setQueryString(string);
        return query;
    }

}
