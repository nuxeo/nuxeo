/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.syndication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.search.SearchActions;
import org.nuxeo.ecm.webapp.search.SearchType;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Syndication bean. This Seam component is used to:
 * <ol>
 * <li>find the syndication URL of the current document or current search,</li>
 * <li>retrieve the actual feed for a URL.</li>
 * </ol>
 * In the first case, we are called in the context of an existing Seam
 * conversation, so an existing navigationContext can be looked up.
 * <p>
 * In the second case, the conversation is a new one.
 *
 * @author Florent Guillaume
 */
@Name("syndication")
@Scope(ScopeType.EVENT)
public class SyndicationActionsBean implements SyndicationActions {

    private static final Log log = LogFactory.getLog(SyndicationActionsBean.class);

    public static final String RSS_TYPE = "rss_2.0";

    public static final String ATOM_TYPE_OLD = "atom_0.3";

    public static final String ATOM_TYPE = "atom_1.0";

    public static final String DEFAULT_TYPE = RSS_TYPE;

    /**
     * Document reference, of the form {@code reponame/documentid}. The
     * deprecated format {@code reponame/1:documentid} is also supported.
     */
    @RequestParameter
    protected String docRef;

    protected static final String DOCREF_KEY = "docRef";

    /**
     * Feed type, see ROME documentation. Usually {@code rss_2.0} or {@code
     * atom_0.3}.
     */
    @RequestParameter
    protected String feedType;

    protected static final String FEEDTYPE_KEY = "feedType";

    /**
     * The search query, expressed in NXQL.
     */
    @RequestParameter
    protected String searchQuery;

    protected static final String SEARCHQUERY_KEY = "searchQuery";

    /**
     * The repository name.
     */
    @RequestParameter
    protected String repositoryId;

    protected static final String REPOSITORYID_KEY = "repositoryId";

    protected static final String DEFAULT_REPOSITORY = "default";

    protected static final String DOCUMENT_SYNDICATION_PATH = "getSyndicationDocument.faces";

    /**
     * Called by rss reader for document-based syndication.
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public void getSyndicationDocument() throws ClientException {
        if (docRef == null || "".equals(docRef)) {
            throw new IllegalArgumentException("Missing docRef");
        }
        if (feedType == null || "".equals(feedType)) {
            feedType = DEFAULT_TYPE;
        }

        /*
         * Parse docRef into serverLocation and docId
         */
        String[] split = docRef.split("/", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid docRef");
        }
        String serverLocation = split[0];
        String docId = split[1];
        if (docId.startsWith("1:")) {
            // deprecated docRef syntax, with DocumentRef type (IdRef assumed)
            docId = docId.substring(2);
            docRef = serverLocation + '/' + docId;
        }
        IdRef idRef = new IdRef(docId);

        // Create a navigationContext from scratch with the proper server
        // location
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        navigationContext.setCurrentServerLocation(new RepositoryLocation(
                serverLocation));
        CoreSession documentManager = navigationContext.getOrCreateDocumentManager();
        DocumentModel doc;
        try {
            doc = documentManager.getDocument(idRef);
        } catch (DocumentSecurityException e) {
            sendForbidden();
            return;
        }

        /*
         * Feed definition
         */
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        String title = (String) doc.getProperty("dublincore", "title");
        if (title == null || "".equals(title)) {
            title = " ";
        }
        feed.setTitle(title);
        String description = (String) doc.getProperty("dublincore",
                "description");
        if (description == null || "".equals(description)) {
            description = " ";
        }
        feed.setDescription(description);

        feed.setLink(getFeedUrl(DOCUMENT_SYNDICATION_PATH, DOCREF_KEY, docRef,
                feedType));

        /*
         * Feed entries
         */
        // skip deleted documents
        // TODO do standard folder children search to check permissions too
        DocumentModelList children = new DocumentModelListImpl();
        for (DocumentModel child : documentManager.getChildren(idRef)) {
            if (LifeCycleConstants.DELETED_STATE.equals(child.getCurrentLifeCycleState())) {
                continue;
            }
            children.add(child);
        }
        List<FeedItem> feedItems = getFeedItems(children);

        // Sort items by update date or if not, by publication date
        Collections.sort(feedItems, Collections.reverseOrder());
        feed.setEntries(feedItems);

        writeFeed(feed);
    }

    private static final String SEARCH_SYNDICATION_PATH = "getSyndicationSearch.faces";

    /**
     * Called by rss reader for search-based syndication.
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public void getSyndicationSearch() throws ClientException {
        if (searchQuery == null || "".equals(searchQuery)) {
            // throw new IllegalArgumentException("Missing searchQuery");
            searchQuery = "";
        }
        searchQuery = searchQuery.replace('\n', ' ').replaceAll(" +", " ");
        if (feedType == null || "".equals(feedType)) {
            feedType = DEFAULT_TYPE;
        }

        if (repositoryId == null || "".equals(repositoryId)) {
            repositoryId = DEFAULT_REPOSITORY;
        }
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        navigationContext.setCurrentServerLocation(new RepositoryLocation(
                repositoryId));

        /*
         * Perform the search
         */
        SearchActions searchActions = (SearchActions) Component.getInstance(
                "searchActions", true);
        searchActions.setSearchTypeId(SearchType.NXQL.name());
        searchActions.setNxql(searchQuery);
        searchActions.performSearch();
        List<DocumentModel> docList = searchActions.getResultDocuments(SearchActions.PROV_NXQL);
        if (docList == null) {
            docList = Collections.emptyList();
        }

        /*
         * Feed definition
         */
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setTitle("Search results");
        feed.setDescription("Query: " + searchQuery);
        feed.setLink(getFeedUrl(SEARCH_SYNDICATION_PATH, SEARCHQUERY_KEY,
                searchQuery, REPOSITORYID_KEY, getRepositoryId(), feedType));

        /*
         * Feed entries
         */
        List<FeedItem> feedItems = getFeedItems(docList);
        feed.setEntries(feedItems);

        writeFeed(feed);
    }

    /**
     * Get feed items given a document list.
     *
     * @param docs the documents
     * @return the feed items
     * @throws ClientException
     */
    protected static List<FeedItem> getFeedItems(List<DocumentModel> docs)
            throws ClientException {
        return FeedItemAdapter.toFeedItemList(docs, null);
    }

    protected static String urlencode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // cannot happen for UTF-8
            throw new RuntimeException(e);
        }
    }

    protected static String getFeedUrl(String path, String key, String value,
            String feedType) {
        return getFeedUrl(path, key, value, null, null, feedType);
    }

    protected static String getFeedUrl(String path, String key1, String value1,
            String key2, String value2, String feedType) {
        StringBuilder url = new StringBuilder();
        url.append(BaseURL.getBaseURL());
        url.append(path);
        url.append('?');
        url.append(key1);
        url.append('=');
        url.append(urlencode(value1));
        if (key2 != null) {
            url.append('&');
            url.append(key2);
            url.append('=');
            url.append(urlencode(value2));
        }
        url.append('&');
        url.append(FEEDTYPE_KEY);
        url.append('=');
        if (feedType != null) {
            url.append(urlencode(feedType));
        } // else UI will append the type by itself
        return url.toString();
    }

    protected static void writeFeed(SyndFeed feed) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setContentType("application/xml; charset=UTF-8");
        try {
            new SyndFeedOutput().output(feed, response.getWriter());
        } catch (Exception e) {
            log.error("Unable to output feed", e);
        }
        context.responseComplete();
    }

    protected static void sendForbidden() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        context.responseComplete();
    }

    protected static String getRepositoryId() {
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        if (navigationContext == null) {
            return DEFAULT_REPOSITORY;
        }
        RepositoryLocation sl = navigationContext.getCurrentServerLocation();
        return sl == null ? DEFAULT_REPOSITORY : sl.getName();
    }

    /**
     * Called by templates to get a documents feed URL.
     */
    @Factory(value = "fullSyndicationDocumentUrl", scope = ScopeType.EVENT)
    public String getFullSyndicationDocumentUrl() {
        return getCurrentDocumentSyndicationUrl(null);
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public String getFullSyndicationDocumentUrlInRss() {
        return getCurrentDocumentSyndicationUrl(RSS_TYPE);
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public String getFullSyndicationDocumentUrlInAtom() {
        return getCurrentDocumentSyndicationUrl(ATOM_TYPE);
    }

    protected static String getCurrentDocumentSyndicationUrl(String feedType) {
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        if (navigationContext == null) {
            return null; // JSF DebugUtil.printTree may call this
        }
        String serverLocation = navigationContext.getCurrentServerLocation().getName();
        String docId = navigationContext.getCurrentDocument().getId();
        String docRef = serverLocation + '/' + docId;
        return getFeedUrl(DOCUMENT_SYNDICATION_PATH, DOCREF_KEY, docRef,
                feedType);
    }

    /**
     * Called by templates to get a search feed URL.
     */
    @Factory(value = "fullSyndicationSearchUrl", scope = ScopeType.EVENT)
    public String getFullSyndicationSearchUrl() {
        return getSearchSyndicationUrl(null);
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public String getFullSyndicationSearchUrlInRss() {
        return getSearchSyndicationUrl(RSS_TYPE);
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public String getFullSyndicationSearchUrlInAtom() {
        return getSearchSyndicationUrl(ATOM_TYPE);
    }

    protected static String getSearchSyndicationUrl(String feedType) {
        SearchActions searchActions = (SearchActions) Component.getInstance(
                "searchActions", true);
        if (searchActions == null) {
            return null; // JSF DebugUtil.printTree may call this
        }
        String searchQuery = searchActions.getLatestNxql();
        if (searchQuery == null) {
            throw new IllegalArgumentException("null searchQuery");
        }
        searchQuery = searchQuery.replace('\n', ' ').replaceAll(" +", " ");
        return getFeedUrl(SEARCH_SYNDICATION_PATH, SEARCHQUERY_KEY,
                searchQuery, REPOSITORYID_KEY, getRepositoryId(), feedType);
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public List<Action> getActionsForSyndication() {
        WebActions webActions = (WebActions) Component.getInstance(
                "webActions", true);
        return webActions.getActionsList("SYNDICATION_LINKS");
    }
}
