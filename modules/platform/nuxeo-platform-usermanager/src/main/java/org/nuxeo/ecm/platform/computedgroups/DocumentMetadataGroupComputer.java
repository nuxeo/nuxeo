/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin Jalon
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Configurable Group Computer based on Metadata of Documents. Documents Selection is managed by NXQL where clause.
 *
 * @since 5.7.3
 */
public class DocumentMetadataGroupComputer extends AbstractGroupComputer {

    public static final Log log = LogFactory.getLog(DocumentMetadataGroupComputer.class);

    private String groupPattern;

    private String whereClause;

    private String xpath;

    public DocumentMetadataGroupComputer(String whereClause, String groupPattern, String xpath) {
        this.whereClause = whereClause;
        this.xpath = xpath;
        this.groupPattern = groupPattern;
        if (whereClause == null || whereClause.isEmpty() || groupPattern == null || groupPattern.isEmpty()) {
            throw new NuxeoException("Bad Contribution Document Metadata Computer Group Configuration");
        }
    }

    @Override
    public List<String> getAllGroupIds() {
        List<String> groupIds = new ArrayList<>();
        return groupIds;
    }

    @Override
    public List<String> getGroupMembers(String groupId) {

        List<String> participants = new ArrayList<>();
        return participants;
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl user) {
        String username = user.getName();
        GetDocumentsFromUsername runner = new GetDocumentsFromUsername(getRepository(), whereClause, username, xpath);
        runner.runUnrestricted();

        List<String> groupIds = new ArrayList<>();
        String groupId = null;

        for (String value : runner.result) {
            groupId = getGroupIdFromValue(value);
            log.debug("Virtual Group Id found: " + groupId);
            groupIds.add(groupId);
        }
        return groupIds;
    }

    @Override
    public List<String> getParentsGroupNames(String groupID) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getSubGroupsNames(String groupID) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasGroup(String groupId) {
        return false;
    }

    protected class GetDocumentsFromUsername extends UnrestrictedSessionRunner {
        private static final String QUERY_PATTERN = "SELECT %s " + "FROM Document %s";

        protected String username;

        protected String xpath;

        protected String whereClausePattern;

        public List<String> result = new ArrayList<>();

        protected GetDocumentsFromUsername(String repositoryName, String whereClause, String username, String xpath) {
            super(repositoryName);
            this.username = username;
            whereClausePattern = whereClause;
            this.xpath = xpath;
        }

        @Override
        public void run() {
            String whereClause = String.format(whereClausePattern, username);
            String query = String.format(QUERY_PATTERN, xpath, whereClause);

            try (IterableQueryResult docs = session.queryAndFetch(query, "NXQL")) {
                for (Map<String, Serializable> doc : docs) {
                    String value = (String) doc.get(xpath);
                    if (value != null && !value.isEmpty() && !result.contains(value)) {
                        result.add(value);
                    }
                }
            }
        }
    }

    private String getRepository() {
        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        return mgr.getDefaultRepositoryName();
    }

    private String getGroupIdFromValue(String value) {
        return String.format(groupPattern, value);
    }

}
