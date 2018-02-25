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
 *     Thomas Roger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("editorLinkActions")
@Scope(CONVERSATION)
public class EditorLinkActionsBean extends InputController implements EditorLinkActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EditorLinkActionsBean.class);

    private List<DocumentModel> resultDocuments;

    private boolean hasSearchResults = false;

    private String searchKeywords;

    @In(create = true, required = false)
    private CoreSession documentManager;

    @Override
    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    @Override
    public List<DocumentModel> getSearchDocumentResults() {
        return resultDocuments;
    }

    @Override
    public String searchDocuments() {
        log.debug("Entering searchDocuments with keywords: " + searchKeywords);

        resultDocuments = null;
        final List<String> constraints = new ArrayList<String>();
        if (searchKeywords != null) {
            searchKeywords = searchKeywords.trim();
            if (searchKeywords.length() > 0) {
                if (!searchKeywords.equals("*")) {
                    // full text search
                    constraints.add(String.format("ecm:fulltext LIKE '%s'", searchKeywords));
                }
            }
        }
        // no folderish doc nor hidden doc
        constraints.add("ecm:mixinType != 'Folderish'");
        constraints.add("ecm:mixinType != 'HiddenInNavigation'");
        // no archived revisions
        constraints.add("ecm:isVersion = 0");
        // search keywords
        final String query = String.format("SELECT * FROM Document WHERE %s",
                StringUtils.join(constraints.toArray(), " AND "));
        log.debug("Query: " + query);

        resultDocuments = documentManager.query(query, 100);
        hasSearchResults = !resultDocuments.isEmpty();
        log.debug("query result contains: " + resultDocuments.size() + " docs.");
        return "editor_link_search_document";
    }

    @Override
    public String getSearchKeywords() {
        return searchKeywords;
    }

    @Override
    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

}
