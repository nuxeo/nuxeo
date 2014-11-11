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
 * $Id: NXSearch.java 21886 2007-07-03 17:56:42Z sfermigier $
 */

package org.nuxeo.ecm.core.search;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.service.SearchServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade for services provided by NXSearch module.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXSearch {

    // Utility class.
    private NXSearch() {
    }

    /**
     * Returns the local search service.
     * <p>
     * Here, the search service is expected to be on the same node. We need this
     * because of the bean wrapping.
     *
     * @return the search service
     */
    public static SearchService getSearchService() {
        return (SearchService) Framework.getRuntime().getComponent(
                SearchServiceImpl.NAME);
    }

}
