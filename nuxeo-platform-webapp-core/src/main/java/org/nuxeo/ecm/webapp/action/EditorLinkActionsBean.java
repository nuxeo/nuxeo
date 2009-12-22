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
 *     Thomas Roger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remove;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.base.InputController;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("editorLinkActions")
@Scope(CONVERSATION)
public class EditorLinkActionsBean extends InputController implements
        EditorLinkActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(EditorLinkActionsBean.class);

    private List<DocumentModel> resultDocuments;

    private boolean hasSearchResults = false;

    private String searchKeywords;

    @In(create = true, required = false)
    private CoreSession documentManager;

    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    public List<DocumentModel> getSearchDocumentResults() {
        return resultDocuments;
    }

    public String searchDocuments() throws ClientException {
        log.debug("Entering searchDocuments with keywords: " + searchKeywords);

        resultDocuments = null;
        final List<String> constraints = new ArrayList<String>();
        if (searchKeywords != null) {
            searchKeywords = searchKeywords.trim();
            if (searchKeywords.length() > 0) {
                if (!searchKeywords.equals("*")) {
                    // full text search
                    constraints.add(String.format("ecm:fulltext LIKE '%s'",
                            searchKeywords));
                }
            }
        }
        // no folderish doc nor hidden doc
        constraints.add("ecm:mixinType != 'Folderish'");
        constraints.add("ecm:mixinType != 'HiddenInNavigation'");
        // no archived revisions
        constraints.add("ecm:isCheckedInVersion = 0");
        // search keywords
        final String query = String.format("SELECT * FROM Document WHERE %s",
                StringUtils.join(constraints.toArray(), " AND "));
        log.debug("Query: " + query);

        resultDocuments = documentManager.query(query, 100);
        hasSearchResults = !resultDocuments.isEmpty();
        log.debug("query result contains: " + resultDocuments.size() + " docs.");
        return "test_popup";
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

}
