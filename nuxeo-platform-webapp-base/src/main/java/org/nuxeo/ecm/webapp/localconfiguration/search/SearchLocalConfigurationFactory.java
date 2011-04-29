/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     eugen
 */
package org.nuxeo.ecm.webapp.localconfiguration.search;

import static org.nuxeo.ecm.webapp.localconfiguration.search.SearchLocalConfigurationConstants.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 */
public class SearchLocalConfigurationFactory implements DocumentAdapterFactory {

    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(SEARCH_LOCAL_CONFIGURATION_FACET)) {
            return new SearchLocalConfigurationAdapter(doc);
        }
        return null;
    }

}
