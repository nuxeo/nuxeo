/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig.gadgets;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.GadgetException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class NXFeedProcessor extends FeedProcessor {

    /**
     * Converts feed XML to JSON.
     * 
     * @param feedUrl The url that the feed was retrieved from.
     * @param feedXml The raw XML of the feed to be converted.
     * @param getSummaries True if summaries should be returned.
     * @param numEntries Number of entries to return.
     * @return The JSON representation of the feed.
     */
    @SuppressWarnings("unchecked")
    public JSONObject process(String feedUrl, String feedXml,
            boolean getSummaries, int numEntries) throws GadgetException {
        try {
            SyndFeed feed = new SyndFeedInput().build(new StringReader(feedXml));
            JSONObject json = new JSONObject();
            json.put("Title", feed.getTitle());
            json.put("URL", feedUrl);
            json.put("Description", feed.getDescription());
            json.put("Link", feed.getLink());

            if (feed.getCategories().size() > 0) {
                json.put("Categories",
                        getCategoriesStringList(feed.getCategories()));
            }

            List<SyndPerson> authors = feed.getAuthors();
            String jsonAuthor = null;
            jsonAuthor = getAuthor(authors);
            JSONArray entries = new JSONArray();
            json.put("Entry", entries);

            int entryCnt = 0;
            for (Object obj : feed.getEntries()) {
                SyndEntry e = (SyndEntry) obj;
                if (entryCnt >= numEntries) {
                    break;
                }
                entryCnt++;

                JSONObject entry = getJSONEntryFromSyndEntry(e, getSummaries);
                entries.put(entry);

                // if no author at feed level, use the first entry author
                if (jsonAuthor == null) {
                    jsonAuthor = e.getAuthor();
                }
            }

            json.put("Author", (jsonAuthor != null) ? jsonAuthor : "");
            return json;
        } catch (JSONException e) {
            // This shouldn't ever happen.
            throw new RuntimeException(e);
        } catch (FeedException e) {
            throw new GadgetException(
                    GadgetException.Code.MALFORMED_XML_DOCUMENT, e);
        }
    }

    private List<String> getCategoriesStringList(List categories) {
        List<String> result = new ArrayList<String>();
        for (Object cat : categories) {
            SyndCategory syndCat = (SyndCategory) cat;
            result.add(syndCat.getName());
        }
        return result;
    }

    private JSONObject getJSONEntryFromSyndEntry(SyndEntry e,
            boolean getSummaries) throws JSONException {
        JSONObject entry = new JSONObject();
        entry.put("Title", e.getTitle());
        entry.put("Link", e.getLink());

        if (e.getCategories().size() > 0) {
            entry.put("Categories", getCategoriesStringList(e.getCategories()));
        }

        if (getSummaries) {
            addSummariesFromEntry(entry, e);
        }

        addDateFromEntry(entry, e);

        joinEnclosureFromEntry(entry, e);

        return entry;
    }

    private void addDateFromEntry(JSONObject entry, SyndEntry e)
            throws JSONException {
        if (e.getUpdatedDate() != null) {
            entry.put("Date", e.getUpdatedDate().getTime());
        } else if (e.getPublishedDate() != null) {
            entry.put("Date", e.getPublishedDate().getTime());
        } else {
            entry.put("Date", 0);
        }

    }

    private void addSummariesFromEntry(JSONObject entry, SyndEntry e)
            throws JSONException {

        if (e.getContents() != null && e.getContents().size() > 0) {
            entry.put("Summary",
                    ((SyndContent) e.getContents().get(0)).getValue());
        } else {
            entry.put("Summary",
                    e.getDescription() != null ? e.getDescription().getValue()
                            : "");
        }

    }

    private String getAuthor(List<SyndPerson> authors) {
        String jsonAuthor = null;
        if (authors != null && !authors.isEmpty()) {
            SyndPerson author = authors.get(0);
            if (author.getName() != null) {
                jsonAuthor = author.getName();
            } else if (author.getEmail() != null) {
                jsonAuthor = author.getEmail();
            }
        }
        return jsonAuthor;
    }

    @SuppressWarnings("unchecked")
    private void joinEnclosureFromEntry(JSONObject entry, SyndEntry e)
            throws JSONException {
        List<SyndEnclosure> enclosuresDefined = e.getEnclosures();
        JSONObject enclosure = new JSONObject();
        if (enclosuresDefined != null && enclosuresDefined.size() > 0) {
            SyndEnclosure enclosureDefined = enclosuresDefined.get(0);
            enclosure.put("Length", enclosureDefined.getLength());
            enclosure.put("URL", enclosureDefined.getUrl());
            enclosure.put("MimeType", enclosureDefined.getType());
        }
        entry.put("Enclosure", enclosure);
    }
}
