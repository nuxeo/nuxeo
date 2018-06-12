/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A command to execute by {@link BulkService}.
 *
 * @since 10.2
 */
public class BulkCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The username which run the bulk command. */
    protected String username;

    protected String repository;

    protected String query;

    /** The bulk action to execute. */
    protected String action;

    public BulkCommand() {
    }

    public BulkCommand withUsername(String username) {
        this.username = username;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public BulkCommand withRepository(String repository) {
        this.repository = repository;
        return this;
    }

    public String getRepository() {
        return repository;
    }

    /**
     * @param query the query used to get the document set concerned by the action
     */
    public BulkCommand withQuery(String query) {
        this.query = query;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public BulkCommand withAction(String action) {
        this.action = action;
        return this;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
