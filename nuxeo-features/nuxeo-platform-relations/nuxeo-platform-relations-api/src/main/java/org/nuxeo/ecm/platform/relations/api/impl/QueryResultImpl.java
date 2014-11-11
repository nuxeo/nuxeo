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
 * $Id: QueryResultImpl.java 20796 2007-06-19 09:52:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;

/**
 * Query results.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class QueryResultImpl implements QueryResult {

    private static final long serialVersionUID = 1L;

    protected Integer count;

    protected List<String> variableNames;

    protected List<Map<String, Node>> results;

    /**
     * Constructor for query result.
     *
     * @param count
     *            integer number of results
     * @param variableNames
     *            list of variable names as requested in query
     * @param results
     *            list of variable names/nodes found mapping
     */
    public QueryResultImpl(Integer count, List<String> variableNames,
            List<Map<String, Node>> results) {
        this.count = count;
        this.variableNames = variableNames;
        this.results = results;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<Map<String, Node>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Node>> results) {
        this.results = results;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }

    public void setVariableNames(List<String> variableNames) {
        this.variableNames = variableNames;
    }

}
