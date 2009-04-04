/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SearchServiceDelegate.java 28583 2008-01-08 20:00:27Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.runtime.api.Framework;

/**
 * Search service stateless delegate.
 * <p>
 * Helper to reach the search service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class SearchServiceDelegate {

    private static final Log log = LogFactory.getLog(SearchServiceDelegate.class);

    // Utility class.
    private SearchServiceDelegate() {
    }

    /**
     * Returns a remote search service.
     * <p>
     * Returns null if an exception occurs.
     * XXX Should return an exception instead
     *
     * @return a search service instance.
     */
    public static SearchService getRemoteSearchService() {
        SearchService service = null;
        try {
            service = Framework.getService(SearchService.class);
        } catch (Exception e) {
            log.error("Failed to lookup distant search service", e);
        }
        return service;
    }

    /**
     * Returns a local search service.
     * <p>
     * Returns null if an exception occurs
     *
     * @return a search service instance
     */
    public static SearchService getLocalSearchService() {
        return Framework.getLocalService(SearchService.class);
    }

}
