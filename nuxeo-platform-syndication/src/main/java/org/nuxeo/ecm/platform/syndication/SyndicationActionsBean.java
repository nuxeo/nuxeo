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
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.syndication;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.RestURLMaker;
import org.nuxeo.ecm.platform.ui.web.util.BadDocumentUriException;
import org.nuxeo.ecm.platform.ui.web.util.DocumentLocator;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.util.ECInvalidParameterException;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.search.SearchActions;
import org.nuxeo.ecm.webapp.search.SearchType;
import org.nuxeo.runtime.api.Framework;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 *
 * @author bchaffangeon
 *
 */
//@Stateless
@Name("syndication")
public class SyndicationActionsBean extends InputController implements
        SyndicationActions {

    public static final String DOC_PARAM_NAME = "docRef";

    public static final String SYNDIC_PARAM_NAME = "feedType";

    public static final String QUERY_PARAM_NAME = "searchQuery";

    public static final String SEARCHTYPE_PARAM_NAME = "searchType";

    private static final Log log = LogFactory.getLog(SyndicationActionsBean.class);

    private static final String MIME_TYPE = "application/xml; charset=UTF-8";

    private static final String SYNDICATE_CATEGORY = "SYNDICATION_LINKS";

    private static final String RSS_TYPE = "rss_2.0";

    private static final String ATOM_TYPE = "atom_0.3";

    private static final String DEFAULT_SEARCH_TYPE = SearchType.KEYWORDS.name();

    private static final String DEFAULT_SYNDICATION_TYPE = RSS_TYPE;

    @In(create = true, required = false)
    private CoreSession documentManager;

    @In(create = true)
    private WebActions webActions;

    @In(create = true, required = false)
    private SearchActions searchActions;

    @In(create = true)
    private RestURLMaker URLMaker;

    @RequestParameter
    private String docRef;

    @RequestParameter
    private String feedType;

    @RequestParameter
    private String searchType;

    @RequestParameter
    private String searchQuery;

    @RequestParameter
    private String providerName;

    // TODO unused
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public List<Action> getActionsForSyndication() {
        return webActions.getActionsList(SYNDICATE_CATEGORY);
    }

    /**
     * Called by URL for document-based syndication.
     *
     * @throws ClientException
     * @throws ParseException
     * @throws FeedException
     * @throws IOException
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public void getSyndicationDocument() throws IOException, FeedException,
            ParseException, ClientException {

        log.debug("Syndication URL called for document");
        if (docRef == null || "".equals(docRef)) {
            log.info("No docRef in request parameter : syndication for current document");
            log.debug("Trying to syndicate current document");
            DocumentRef ref = navigationContext.getCurrentDocument().getRef();
            docRef = navigationContext.getCurrentServerLocation().getName();
            docRef += "/";
            docRef += ref.type();
            docRef += ":";
            docRef += ref;
        }
        if (feedType == null || "".equals(feedType)) {
            log.info("No syndication type in request parameter : set by default to"
                    + DEFAULT_SYNDICATION_TYPE);
            feedType = DEFAULT_SYNDICATION_TYPE;
        }
        syndicateDocument();
    }

    /**
     * Called by URL for search-based syndication.
     *
     * @throws ClientException
     * @throws ParseException
     * @throws FeedException
     * @throws IOException
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public void getSyndicationSearch() throws ClientException, IOException,
            FeedException, ParseException, ECInvalidParameterException {

        log.debug("Syndication URL called for search results");
        if (providerName == null || "".equals(providerName)) {
            // XXX : Quick fix
            log.warn("No providerName in request parameter, using SIMPLE_SEARCH");
            providerName="SIMPLE_SEARCH";
            //throw new ClientException("no providerName in request parameter");
        }
        if (searchQuery == null || "".equals(searchQuery)) {
            log.debug("No search query in request parameter");
            searchQuery = "";
        }
        if (searchType == null || "".equals(searchType)) {
            log.debug("No search type in request parameter : set by default to"
                    + DEFAULT_SEARCH_TYPE);
            searchType = DEFAULT_SEARCH_TYPE;
        }
        if (feedType == null || "".equals(feedType)) {
            log.debug("No syndication type in request parameter : set by default to"
                    + DEFAULT_SYNDICATION_TYPE);
            feedType = DEFAULT_SYNDICATION_TYPE;
        }
        syndicateSearch(providerName);
    }

    /**
     * Writes the feed in Servlet Response.
     *
     * @param feed
     * @throws IOException
     * @throws FeedException
     */
    private void writeFeed(SyndFeed feed) throws IOException, FeedException {

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        log.debug("Writing feed with type " + feedType);
        // log.debug(feed);

        response.setContentType(MIME_TYPE);
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            output.output(feed, response.getWriter());
        } catch (Exception e) {
            log.error("Unable to output feed :" + e.getMessage());
            log.debug(e.getStackTrace().toString());
        }
        context.responseComplete();
    }

    /**
     * Creates the feed.
     *
     * @param items
     * @return
     */
    protected SyndFeed getSyndFeed(List<FeedItem> items) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setEntries(items);
        return feed;
    }

    /**
     * @param feed is the SyndFeed to initialize
     * @return the initialized SyndFeed used for Document syndication
     * @throws ClientException
     */
    public SyndFeed initializeFeedForDocument(SyndFeed feed)
            throws ClientException {

        log.debug("Initializing feed for document syndication");

        DocumentRef document = getSyndicationRoot(docRef).getRef();
        DocumentModel docModel = documentManager.getDocument(document);

        feed.setFeedType(feedType);
        String feedTitle =(String) docModel.getProperty("dublincore", "title");
        if (feedTitle==null || "".equals(feedTitle))
        {
            feedTitle=" ";
        }
        feed.setTitle(feedTitle);

        String documentLocation = URLMaker.getDocumentBaseUrl();
        Map<String, String> param = new HashMap<String, String>();

        param.put(DOC_PARAM_NAME,
                SyndicationLocator.getSyndicationDocumentUrl(navigationContext));
        param.put(SYNDIC_PARAM_NAME, feedType);

        feed.setLink(SyndicationLocator.getFullSyndicationDocumentUrl(
                documentLocation, param));

        String description = (String) docModel.getProperty("dublincore",
                "description");
        if (description != null) {
            feed.setDescription(description);
        } else {
            feed.setDescription(" ");
        }

        return feed;
    }

    /**
     *
     * @param feed the SyndFeed to initialize
     * @param searchQuery the search query, fulltext or NXSQL, depending on
     *            param type
     * @param searchType the type of the search
     */

    private void initializeFeedForSearch(SyndFeed feed) {
        log.debug("Initializing feed for search results syndication");
        feed.setFeedType(feedType);
        feed.setTitle("Search results");
        feed.setDescription("Query : " + searchQuery);

        String documentLocation = URLMaker.getDocumentBaseUrl();

        Map<String, String> param = new HashMap<String, String>();

        param.put(QUERY_PARAM_NAME, searchQuery);
        param.put(SEARCHTYPE_PARAM_NAME, searchType);
        param.put(SYNDIC_PARAM_NAME, feedType);

        /*
         * feed.setLink(SyndicationLocator.getFullSyndicationDocumentUrl(
         * documentLocation, param));
         */

        feed.setLink(SyndicationLocator.getFullSyndicationSearchUrl(
                documentLocation, param));
    }

    @Factory(value="fullSyndicationDocumentUrl", scope=ScopeType.EVENT)
    public String getFullSyndicationDocumentUrl() {
        return SyndicationLocator.getFullSyndicationDocumentUrl(
                navigationContext, null);
    }

    public String getFullSyndicationDocumentUrlInRss() {
        return SyndicationLocator.getFullSyndicationDocumentUrl(
                navigationContext, RSS_TYPE);
    }

    public String getFullSyndicationDocumentUrlInAtom() {
        return SyndicationLocator.getFullSyndicationDocumentUrl(
                navigationContext, ATOM_TYPE);
    }

    /**
     * Returns the right document to syndicate, provided in URL.
     */
    private DocumentModel getSyndicationRoot(String url) throws ClientException {

        final DocumentLocation docLoc;
        try {
            docLoc = DocumentLocator.parseDocRef(url);
        } catch (BadDocumentUriException e) {
            log.error("Cannot get document ref from uri " + url + ". "
                    + e.getMessage(), e);
            return null;
        }

        documentManager = getDocumentManager(docLoc.getServerLocationName());
        return documentManager.getDocument(docLoc.getDocRef());
    }

    @Remove
    public void destroy() {
        log.debug("Removing Seam component: syndication");
    }

    protected DocumentViewCodecManager getDocumentViewCodecService()
            throws ClientException {
        try {
            return Framework.getService(DocumentViewCodecManager.class);
        } catch (Exception e) {
            throw new ClientException(
                    "Could not get DocumentViewCodec service", e);
        }
    }

    public void syndicateSearch(String providerName) throws ClientException,
            IOException, FeedException, ParseException,
            ECInvalidParameterException {

        // TODO GR: searchActions should be restricted for direct through
        // the-web interaction with the user.
        // The syndication bean should fire its Search Service requests
        // directly

        searchActions.setSearchTypeId(searchType);
        searchActions.setNxql(searchQuery);
        searchActions.performSearch();
        log.debug("Trying to syndicate search results with param : searchType="
                + searchType + ", query=" + searchQuery);
        log.debug("Results number : "
                + Integer.toString(searchActions.getResultDocuments(
                        providerName).size()));

        // Get the result search
        DocumentModelList docList = new DocumentModelListImpl();

        if (searchActions.getResultDocuments(providerName) != null
                && !searchActions.getResultDocuments(providerName).isEmpty()) {

            docList = getRealDocuments(new DocumentModelListImpl(
                    searchActions.getResultDocuments(providerName)));
        }

        FeedItemAdapter feedItemAdapt = new FeedItemAdapter();
        List<FeedItem> feedItems;

        feedItems = feedItemAdapt.toFeedItemList(docList);

        SyndFeed feed = getSyndFeed(feedItems);
        initializeFeedForSearch(feed);
        writeFeed(feed);
    }

    public void syndicateDocument() throws ClientException, IOException,
            FeedException, ParseException {

        FeedItemAdapter feedItemAdapt = new FeedItemAdapter();
        List<FeedItem> feedItems;

        DocumentRef document = getSyndicationRoot(docRef).getRef();
        DocumentModelList allChilds = getRealDocuments(documentManager.getChildren(document));

        feedItems = feedItemAdapt.toFeedItemList(allChilds);

        // Sort items by update date or if not, by publication date
        Collections.sort(feedItems, Collections.reverseOrder());

        // Write full feed
        SyndFeed feed = getSyndFeed(feedItems);
        initializeFeedForDocument(feed);
        writeFeed(feed);
    }

    /**
     * Get the real document instead of a document with a null session id.
     * Because in that case, we can't get any information about the doc.
     */
    private DocumentModelList getRealDocuments(DocumentModelList childs)
            throws ClientException {
        DocumentModelList allChilds = new DocumentModelListImpl();
        for (DocumentModel child : childs) {
            if (child.getRef() != null) {
                String repoName = child.getRepositoryName();
                documentManager = getDocumentManager(repoName);
                allChilds.add(documentManager.getDocument(child.getRef()));
            }
        }
        return allChilds;
    }

    @Factory(value="fullSyndicationSearchUrl", scope=ScopeType.EVENT)
    public String getFullSyndicationSearchUrl() {
        return SyndicationLocator.getFullSyndicationSearchUrl(
                navigationContext, searchActions.getLatestNxql(),
                SearchType.NXQL.name(), null);
    }

    public String getFullSyndicationSearchUrlInAtom() {
        return SyndicationLocator.getFullSyndicationSearchUrl(
                navigationContext, searchActions.getLatestNxql(),
                SearchType.NXQL.name(), ATOM_TYPE);
    }

    public String getFullSyndicationSearchUrlInRss() {
        return SyndicationLocator.getFullSyndicationSearchUrl(
                navigationContext, searchActions.getLatestNxql(),
                SearchType.NXQL.name(), RSS_TYPE);
    }


    private CoreSession getDocumentManager(String repositoryName) throws ClientException
    {
        if (navigationContext.getCurrentServerLocation() == null) {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repositoryName));
        }

        if (documentManager == null || !documentManager.getRepositoryName().equals(repositoryName)) {
            documentManager = navigationContext.getOrCreateDocumentManager();
        }

        return documentManager;
    }

    /*
    private CoreSession getDocumentManager() throws ClientException {
        if (documentManager == null) {
            documentManager = (CoreSession) Component.getInstance(
                    "documentManager", true);
        }
        if (documentManager==null)
        {
            DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts("documentManager");
            if (documentManagerBD == null) {
                documentManagerBD = new DocumentManagerBusinessDelegate();
            }
            RepositoryLocation serverLoc = getEditedRepositoryLocation();
            documentManager = documentManagerBD.getDocumentManager(serverLoc);
            Contexts.getConversationContext().set("currentServerLocation", serverLoc);
        }
        return documentManager;
    }
    */
}
