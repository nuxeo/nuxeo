/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.net.URLEncoder;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Basic OpenSearch REST fulltext search implementation using the RSS 2.0
 * results format.
 * <p>
 * TODO: make it possible to change the page size and navigate to next results
 * pages using additional query parameters. See http://opensearch.org for
 * official specifications.
 * <p>
 * TODO: use a OPENSEARCH stateless query model to be able to override the
 * currently hardcoded request pattern.
 * <p>
 * TODO: add OpenSearch XML description snippet in the default theme so that
 * Firefox can autodetect the service URL.
 *
 * @author Olivier Grisel
 */
public class OpenSearchRestlet extends BaseNuxeoRestlet {

    public static final String RSS_TAG = "rss";

    public static final String CHANNEL_TAG = "channel";

    public static final String TITLE_TAG = "title";

    public static final String DESCRIPTION_TAG = "description";

    public static final String LINK_TAG = "link";

    public static final String ITEM_TAG = "item";

    public static final Namespace OPENSEARCH_NS = new Namespace("opensearch",
            "http://a9.com/-/spec/opensearch/1.1/");

    public static final Namespace ATOM_NS = new Namespace("atom",
            "http://www.w3.org/2005/Atom");

    @Override
    public void handle(Request req, Response res) {

        try {
            // read the search term passed as the 'q' request parameter
            String keywords = getQueryParamValue(req, "q", " ");

            // perform the search on the fulltext index and wrap the results as
            // a DocumentModelList with the 10 first matching results ordered by
            // modification time
            SearchService searchService = Framework.getService(SearchService.class);
            ComposedNXQuery query = new ComposedNXQueryImpl(
                    String.format(
                            "SELECT * FROM Document WHERE ecm:fulltext LIKE '%s' ORDER BY dc:modified DESC",
                            keywords));
            ResultSet resultSet = searchService.searchQuery(query, 0, 10);
            SearchPageProvider pageProvider = new SearchPageProvider(resultSet);
            DocumentModelList documents = pageProvider.getCurrentPage();

            // build the RSS 2.0 response document holding the results
            DOMDocumentFactory domfactory = new DOMDocumentFactory();
            DOMDocument resultDocument = (DOMDocument) domfactory.createDocument();

            // rss root tag
            Element rssElement = resultDocument.addElement(RSS_TAG);
            rssElement.addAttribute("version", "2.0");
            rssElement.addNamespace(OPENSEARCH_NS.getPrefix(),
                    OPENSEARCH_NS.getURI());
            rssElement.addNamespace(ATOM_NS.getPrefix(), ATOM_NS.getURI());

            // channel with OpenSearch metadata
            Element channelElement = rssElement.addElement(CHANNEL_TAG);

            channelElement.addElement(TITLE_TAG).setText(
                    "Nuxeo EP OpenSearch channel for " + keywords);
            channelElement.addElement("link").setText(
                    BaseURL.getBaseURL(getHttpRequest(req))
                            + "restAPI/opensearch?q="
                            + URLEncoder.encode(keywords, "UTF-8"));
            channelElement.addElement(new QName("totalResults", OPENSEARCH_NS)).setText(
                    Long.toString(pageProvider.getResultsCount()));
            channelElement.addElement(new QName("startIndex", OPENSEARCH_NS)).setText(
                    Integer.toString(pageProvider.getCurrentPageOffset()));
            channelElement.addElement(new QName("startIndex", OPENSEARCH_NS)).setText(
                    Integer.toString(pageProvider.getPageSize()));

            Element queryElement = channelElement.addElement(new QName("Query",
                    OPENSEARCH_NS));
            queryElement.addAttribute("role", "request");
            queryElement.addAttribute("searchTerms", keywords);
            queryElement.addAttribute("startPage", Integer.toString(1));

            // document items
            String baseUrl = BaseURL.getBaseURL(getHttpRequest(req));

            for (DocumentModel doc : documents) {
                Element itemElement = channelElement.addElement(ITEM_TAG);
                Element titleElement = itemElement.addElement(TITLE_TAG);
                String title = doc.getTitle();
                if (title != null) {
                    titleElement.setText(title);
                }
                Element descriptionElement = itemElement.addElement(DESCRIPTION_TAG);
                String description = doc.getProperty("dublincore:description").getValue(
                        String.class);
                if (description != null) {
                    descriptionElement.setText(description);
                }
                Element linkElement = itemElement.addElement("link");
                linkElement.setText(baseUrl
                        + DocumentModelFunctions.documentUrl(doc));
            }

            res.setEntity(resultDocument.asXML(), MediaType.TEXT_XML);

        } catch (Exception e) {
            handleError(res, e);
        }
    }

}
