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
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.search.delegate;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class RelationSearchBusinessDelegate {


    /**
     * Lookups relation manager.
     *
     * TODO true remote lookup, pools, etc.
     *
     * @return the relation manager
     */
    public static RelationManager getRelationManager() {
        try {
            return Framework.getService(RelationManager.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }
    }

    public static SearchService getSearchService() {
        return SearchServiceDelegate.getRemoteSearchService();
    }

}
