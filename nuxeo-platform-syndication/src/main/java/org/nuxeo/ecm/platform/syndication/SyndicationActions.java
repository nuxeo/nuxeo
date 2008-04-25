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
 *     Brice Chaffangeon
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.syndication;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.actions.Action;

/**
 * Syndication bean. This Seam component is used to:
 * <ol>
 * <li>find the syndication URL of the current document or current search,</li>
 * <li>retrieve the actual feed for a URL.</li>
 * </ol>
 *
 * @author Brice Chaffangeon
 * @author Florent Guillaume
 */
public interface SyndicationActions {

    /**
     * Called by rss reader for document-based syndication.
     */
    void getSyndicationDocument() throws ClientException;

    /**
     * Called by rss reader for search-based syndication.
     */
    void getSyndicationSearch() throws ClientException;

    /**
     * Called by templates to get a documents feed URL.
     */
    String getFullSyndicationDocumentUrl();

    /**
     * @deprecated Unused
     */
    @Deprecated
    String getFullSyndicationDocumentUrlInRss();

    /**
     * @deprecated Unused
     */
    @Deprecated
    String getFullSyndicationDocumentUrlInAtom();

    /**
     * Called by templates to get a search feed URL.
     */
    String getFullSyndicationSearchUrl();

    /**
     * @deprecated Unused
     */
    @Deprecated
    String getFullSyndicationSearchUrlInRss();

    /**
     * @deprecated Unused
     */
    @Deprecated
    String getFullSyndicationSearchUrlInAtom();

    /**
     * @deprecated Unused
     */
    @Deprecated
    List<Action> getActionsForSyndication();
}
