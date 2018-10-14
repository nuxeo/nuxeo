/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.net.URLEncoder;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

/**
 * Basic OpenSearch REST fulltext search implementation using the RSS 2.0 results format.
 * <p>
 * TODO: make it possible to change the page size and navigate to next results pages using additional query parameters.
 * See http://opensearch.org for official specifications.
 * <p>
 * TODO: use a OPENSEARCH stateless query model to be able to override the currently hardcoded request pattern.
 * <p>
 * TODO: add OpenSearch XML description snippet in the default theme so that Firefox can autodetect the service URL.
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

    public static final String QUERY = "SELECT * FROM Document WHERE ecm:fulltext LIKE '%s' ORDER BY dc:modified DESC";

    public static final int MAX = 10;

    public static final Namespace OPENSEARCH_NS = new Namespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");

    public static final Namespace ATOM_NS = new Namespace("atom", "http://www.w3.org/2005/Atom");

    @Override
    public void handle(Request req, Response res) {
        try (CloseableCoreSession session = CoreInstance.openCoreSession(null)) {
            // read the search term passed as the 'q' request parameter
            String keywords = getQueryParamValue(req, "q", " ");

            // perform the search on the fulltext index and wrap the results as
            // a DocumentModelList with the 10 first matching results ordered by
            // modification time
            String query = String.format(QUERY, NXQL.escapeStringInner(keywords));
            DocumentModelList documents = session.query(query, null, MAX, 0, true);

            // build the RSS 2.0 response document holding the results
            DOMDocumentFactory domFactory = new DOMDocumentFactory();
            DOMDocument resultDocument = (DOMDocument) domFactory.createDocument();

            // rss root tag
            Element rssElement = resultDocument.addElement(RSS_TAG);
            rssElement.addAttribute("version", "2.0");
            rssElement.addNamespace(OPENSEARCH_NS.getPrefix(), OPENSEARCH_NS.getURI());
            rssElement.addNamespace(ATOM_NS.getPrefix(), ATOM_NS.getURI());

            // channel with OpenSearch metadata
            Element channelElement = rssElement.addElement(CHANNEL_TAG);

            channelElement.addElement(TITLE_TAG).setText("Nuxeo EP OpenSearch channel for " + keywords);
            channelElement.addElement("link").setText(
                    BaseURL.getBaseURL(getHttpRequest(req)) + "restAPI/opensearch?q="
                            + URLEncoder.encode(keywords, "UTF-8"));
            channelElement.addElement(new QName("totalResults", OPENSEARCH_NS)).setText(
                    Long.toString(documents.totalSize()));
            channelElement.addElement(new QName("startIndex", OPENSEARCH_NS)).setText("0");
            channelElement.addElement(new QName("itemsPerPage", OPENSEARCH_NS)).setText(
                    Integer.toString(documents.size()));

            Element queryElement = channelElement.addElement(new QName("Query", OPENSEARCH_NS));
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
                String description = doc.getProperty("dublincore:description").getValue(String.class);
                if (description != null) {
                    descriptionElement.setText(description);
                }
                Element linkElement = itemElement.addElement("link");
                linkElement.setText(baseUrl + DocumentModelFunctions.documentUrl(doc));
            }

            Representation rep = new StringRepresentation(resultDocument.asXML(), MediaType.APPLICATION_XML);
            rep.setCharacterSet(CharacterSet.UTF_8);
            res.setEntity(rep);

        } catch (NuxeoException | IOException e) {
            handleError(res, e);
        }
    }

}
