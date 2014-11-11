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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.webengine.sites.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.*;
import static org.nuxeo.webengine.sites.utils.SiteConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.webengine.sites.utils.SitesRelationsWikiHelper;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class SitesWikiListener implements EventListener {

    private static final Log log = LogFactory.getLog(SitesWikiListener.class);

    private final String openBracket = "[";

    private final String closeBracket = "]";

    private final String imageTag = "image:";

    public void handleEvent(Event event) {
        String eventName = event.getName();

        if (!(DOCUMENT_UPDATED.equals(eventName)
                || DOCUMENT_CREATED.equals(eventName)
                || BEFORE_DOC_UPDATE.equals(eventName) || ABOUT_TO_CREATE.equals(eventName))) {
            return;
        }

        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }

        DocumentModel webPage = docCtx.getSourceDocument();
        if (webPage == null || !WEBPAGE.equals(webPage.getType())) {
            return;
        }

        try {
            Boolean isRichText = (Boolean) webPage.getPropertyValue(WEBPAGE_EDITOR);

            if (isRichText) {
                return;
            }

            WikiProcessingResult result = new WikiProcessingResult();
            if (BEFORE_DOC_UPDATE.equals(event.getName())
                    || ABOUT_TO_CREATE.equals(event.getName())) {
                String wikiContent = (String) webPage.getPropertyValue(WEBPAGE_CONTENT);
                String basePath = (String) webPage.getContextData("basePath");
                if (basePath == null) {
                    basePath = "/nuxeo/site/sites";
                }
                String targetObjectPath = (String) webPage.getContextData("targetObjectPath");

                result = processWikiContent(wikiContent, basePath, targetObjectPath);
                webPage.setPropertyValue(WEBPAGE_CONTENT, result.wikiContent);
                SitesRelationsWikiHelper.updateRelations(webPage, result.relationLinks);
            }

        } catch (ClientException e) {
            log.error("SitesWikiListener error...", e);
        }
    }

    public class WikiProcessingResult {

        protected final List<String> relationLinks = new ArrayList<String>();

        protected String wikiContent;

        public List<String> getRelationLinks() {
            return relationLinks;
        }

        public String getWikiContent() {
            return wikiContent;
        }

    }

    public WikiProcessingResult processWikiContent(String wikiContent, String basePath, String targetObjectPath) {

        WikiProcessingResult result = new WikiProcessingResult();

        String[] wikiLinks = getLinks(wikiContent);

        for (int i = 0; i < wikiLinks.length; i++) {
            String[] splitWikiLinks = StringUtils.split(wikiLinks[i]);
            String linkString = splitWikiLinks[0];
            boolean isImageLink=false;
            String orgLinkString = linkString;
            if (linkString.startsWith(imageTag)) {
                isImageLink=true;
                linkString = linkString.substring(6);
            }

            boolean isNamedLink = splitWikiLinks.length > 1;
            if (!(linkString.startsWith("http://") || linkString.startsWith("https://") || linkString.startsWith(basePath))) {
                // Not an absolute link or not already processed.
                String newLinkString;
                if (linkString.startsWith(".")) {
                    // Absolute path
                    // Just replace . with /
                    newLinkString = basePath
                            + linkString.replace(".", "/");
                } else {
                    // Relative path
                    if (targetObjectPath!=null) {
                        newLinkString = linkString.replace(".", "/");
                        newLinkString = targetObjectPath + "/"
                            + newLinkString;
                    }
                    else {
                        // can not process for now ...
                        newLinkString = linkString;
                    }
                }
                result.relationLinks.add(newLinkString);
                if (!isNamedLink) {
                    newLinkString = newLinkString + " " + linkString;
                }
                if (isImageLink) {
                    newLinkString = imageTag + newLinkString;
                }
                wikiContent = wikiContent.replace(orgLinkString,
                        newLinkString);
            }

            if (linkString.startsWith(basePath)) {
                result.relationLinks.add(linkString);
            }
        }

        result.wikiContent = wikiContent;
        return result;
    }

    private String[] getLinks(String content) {
        if (content == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        int contentLength = content.length();
        if (contentLength == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        List<String> returnList = new ArrayList<String>();
        int currentPosition = 0;
        while (currentPosition < (contentLength - 1)) {
            int startPosition = content.indexOf(openBracket, currentPosition);
            if (startPosition < 0) {
                break;
            }
            startPosition++;
            int endPosition = content.indexOf(closeBracket, startPosition);
            if (endPosition < 0) {
                break;
            }
            returnList.add(content.substring(startPosition, endPosition));
            currentPosition = endPosition + 1;
        }
        if (!returnList.isEmpty()) {
            return returnList.toArray(new String[returnList.size()]);
        } else {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

    }

}
