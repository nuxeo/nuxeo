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
 *     bchaffangeon
 *
 */

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.syndication.FeedItem;
import org.nuxeo.ecm.platform.syndication.FeedItemAdapter;
import org.nuxeo.ecm.platform.syndication.workflow.DashBoardItem;
import org.restlet.data.Response;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * @author bchaffangeon
 */
public class AbstractSyndicationSerializer extends
        AbstractDocumentModelSerializer {

    private String syndicationFormat = "rss_2.0";

    private static final String syndicationURL
            = "http://localhost:8080/nuxeo/restAPI/{repoId}/{docId}/{format}";

    @Override
    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) {

        if (docList == null) {
            return EMPTY_LIST;
        }
        // Builds syndication entries
        List<FeedItem> syndicationEntries;
        try {
            syndicationEntries = FeedItemAdapter.toFeedItemList(docList, req);
        } catch (Exception e) {
            return null;
        }

        // Builds the syndication feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(syndicationFormat);

        // Initializes feed
        feed.setTitle(summary.getTitle());
        // feed.setLink((DocumentModelFunctions.documentUrl(null, folder, null,
        // null, true)));
        // String url = getSyndicationURL(folder.getRepositoryName(),
        // folder.getRef().toString());
        feed.setLink(summary.getLink());

        if (summary.getDescription() != null) {
            feed.setDescription(summary.getDescription());
        } else {
            feed.setDescription(" ");
        }

        feed.setAuthor(summary.getAuthor());
        feed.setPublishedDate(summary.getModificationDate());

        feed.setEntries(syndicationEntries);

        SyndFeedOutput output = new SyndFeedOutput();

        // Try to return feed
        try {
            return output.outputString(feed);
        } catch (FeedException fe) {
            return null;
        }
    }

    public String getSyndicationFormat() {
        return syndicationFormat;
    }

    public void setSyndicationFormat(String syndicationFormat) {
        this.syndicationFormat = syndicationFormat;
    }

    /**
     * Build correct URL for backend XML feed.
     */
    public String getSyndicationURL(String repository, String docId) {
        String url = syndicationURL.replace("{docId}", docId);
        url = url.replace("{repoId}", repository);
        url = url.replace("{format}", syndicationFormat);
        return url;
    }

    public static String serialize(List<DashBoardItem> workItems,
            String columnsDefinition, Map<String, String> options, Response res, HttpServletRequest req) {
        // To be overridden
        return null;
    }

}
