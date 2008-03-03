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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.util.Arrays;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.QueryResult;

public class JCRQueryXPath implements Query {

    private static final Log log = LogFactory.getLog(JCRQuery.class);

    private final JCRSession session;

    private final String rawQuery;

    private final String[] queryParams;

    public JCRQueryXPath(JCRSession session, String query, String... params) {
        rawQuery = query;
        queryParams = params;
        this.session = session;
    }


    public JCRSession getSession() {
        return session;
    }

    public QueryResult execute() throws QueryException {
        log.debug("execute XPath query: " + rawQuery + ", with params: " + Arrays.asList(queryParams));
        try {
            final QueryManager qm = session.jcrSession().getWorkspace()
                    .getQueryManager();

            final String fullQuery = replaceInParams();

            log.debug("execute XPath fullQuery: " + fullQuery);

            final String xpathQuery = translatePath(fullQuery);

            log.debug("execute xpathQuery: " + xpathQuery);

            final javax.jcr.query.Query qry = qm.createQuery(xpathQuery,
                    javax.jcr.query.Query.XPATH);
            //log.info("jcr Query: " + qry.getStatement());
            //sqlQuery = SQLQueryParser.parse(rawQuery);
            //jcrQuery = buildJcrQuery(sqlQuery);
            return new JCRQueryXPathResult(this, qry.execute());
        } catch (RepositoryException e) {
            throw new QueryException("Failed to execute query", e);
        } catch (QueryParseException e) {
            throw new QueryException(e);
        }
    }

    private String replaceInParams() {
        String fullQuery = rawQuery;

        for (String param : queryParams) {
            final String encParam;
            if (param == null) {
                log.warn("NULL parameter for nxql query");
                encParam = "null";
            } else {
                encParam = org.apache.jackrabbit.util.ISO9075.encode(param);
            }
            fullQuery = fullQuery.replaceFirst("\\?", encParam);
        }

        return fullQuery;
    }

    private static String translatePath(String rawQuery) {
        if (rawQuery.startsWith("//element(")) {
            // leave it so ::
            // XXX the full text search for extracted won't work from a
            // subpath, but only starting from root
            // Need to investigate
            return rawQuery;
        }
        // TODO need a real xpath query parser
        // "//element(*, ecmnt:document)[jcr:contains(.,'*" + keywords + "*')]"

        // extract the path from xpath query
        int pathEndIndex = rawQuery.indexOf("element");
        String path = rawQuery.substring(0, pathEndIndex);
        String rest = rawQuery.substring(pathEndIndex);
        //log.info("path: " + path);

        // build the real path
        final StringBuilder buf = new StringBuilder();
        buf.append(buildJcrPath(path));
        buf.append(rest);

        return buf.toString();
    }

    /**
     * Transforms the ECM path to a JCR real path.
     * <p>
     * Ex: from '/testfolder1/testfolder2/'
     * creates a path like
     * '/ecm:root/ecm:children/testfolder1/ecm:children/testfolder2'
     * @param s
     * @return
     */
    private static String buildJcrPath(String s) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("//");
        buffer.append(NodeConstants.ECM_ROOT.rawname).append('/');
        buffer.append(ModelAdapter.path2Jcr(new Path(s.replaceAll("\\.", "/"))));
        buffer.append('/');
        return buffer.toString();
    }

}
