/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class QueryResultImpl implements QueryResult {

    private static final long serialVersionUID = 1L;

    protected Integer count;

    protected List<String> variableNames;

    protected List<Map<String, Node>> results;

    /**
     * Constructor for query result.
     *
     * @param count integer number of results
     * @param variableNames list of variable names as requested in query
     * @param results list of variable names/nodes found mapping
     */
    public QueryResultImpl(Integer count, List<String> variableNames, List<Map<String, Node>> results) {
        this.count = count;
        this.variableNames = variableNames;
        this.results = results;
    }

    @Override
    public Integer getCount() {
        return count;
    }

    @Override
    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public List<Map<String, Node>> getResults() {
        return results;
    }

    @Override
    public void setResults(List<Map<String, Node>> results) {
        this.results = results;
    }

    @Override
    public List<String> getVariableNames() {
        return variableNames;
    }

    @Override
    public void setVariableNames(List<String> variableNames) {
        this.variableNames = variableNames;
    }

}
