/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Configurable Group Computer based on Metadata of Documents. Documents
 * Selection is managed by NXQL where clause.
 *
 * @since 5.7.3
 */
public class DocumentMetadataGroupComputer extends AbstractGroupComputer {

    public static final Log log = LogFactory.getLog(DocumentMetadataGroupComputer.class);

    private String groupPattern;

    private String whereClause;

    private String xpath;

    public DocumentMetadataGroupComputer(String whereClause,
            String groupPattern, String xpath) throws ClientException {
        this.whereClause = whereClause;
        this.xpath = xpath;
        this.groupPattern = groupPattern;
        if (whereClause == null || whereClause.isEmpty()
                || groupPattern == null || groupPattern.isEmpty()) {
            throw new ClientException(
                    "Bad Contribution Document Metadata Computer Group Configuration");
        }
    }

    @Override
    public List<String> getAllGroupIds() throws Exception {
        List<String> groupIds = new ArrayList<String>();
        return groupIds;
    }

    @Override
    public List<String> getGroupMembers(String groupId) throws Exception {

        List<String> participants = new ArrayList<String>();
        return participants;
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl user)
            throws Exception {
        String username = user.getName();
        GetDocumentsFromUsername runner = new GetDocumentsFromUsername(
                getRepository(), whereClause, username, xpath);
        runner.runUnrestricted();

        List<String> groupIds = new ArrayList<String>();
        String groupId = null;

        for (String value : runner.result) {
            groupId = getGroupIdFromValue(value);
            log.debug("Virtual Group Id found: " + groupId);
            groupIds.add(groupId);
        }
        return groupIds;
    }

    @Override
    public List<String> getParentsGroupNames(String groupID) throws Exception {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getSubGroupsNames(String groupID) throws Exception {
        return new ArrayList<String>();
    }

    @Override
    public boolean hasGroup(String groupId) throws Exception {
        return false;
    }

    protected class GetDocumentsFromUsername extends UnrestrictedSessionRunner {
        private static final String QUERY_PATTERN = "SELECT %s "
                + "FROM Document %s";

        protected String username;

        protected String xpath;

        protected String whereClausePattern;

        public List<String> result = new ArrayList<String>();

        protected GetDocumentsFromUsername(String repositoryName,
                String whereClause, String username, String xpath)
                throws Exception {
            super(repositoryName);
            this.username = username;
            whereClausePattern = whereClause;
            this.xpath = xpath;
        }

        @Override
        public void run() throws ClientException {
            String whereClause = String.format(whereClausePattern, username);
            String query = String.format(QUERY_PATTERN, xpath, whereClause);

            IterableQueryResult docs = session.queryAndFetch(query, "NXQL");
            for (Map<String, Serializable> doc : docs) {
                String value = (String) doc.get(xpath);
                if (value != null && !value.isEmpty()
                        && !result.contains(value)) {
                    result.add(value);
                }
            }

        }
    }

    private String getRepository() {
        RepositoryManager mgr = Framework.getLocalService(RepositoryManager.class);
        return mgr.getDefaultRepositoryName();
    }

    private String getGroupIdFromValue(String value) {
        return String.format(groupPattern, value);
    }

}
