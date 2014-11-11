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

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 * Interface for search local configuration
 *
 */
public interface SearchLocalConfiguration extends LocalConfiguration<SearchLocalConfiguration>{

    /**
     * @return name of the content view used in advanced search page
     */
    String getAdvancedSearchView();

}
