/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.search.ui.localconfiguration;

import java.util.List;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * @since 5.9.6
 */
public interface SearchConfiguration extends
        LocalConfiguration<SearchConfiguration> {

    /**
     * Return a list of content views name that are denied with the local
     * configuration
     *
     * @return an unmodifiable list of String or null.
     */
    List<String> getAllowedContentViewNames();

    /**
     * Provide a filter to remove unauthorized content views name.
     *
     * @param names set of possible content views name
     * @return a set without unauthorised content views, it should be empty.
     */
    List<String> filterAllowedContentViewNames(List<String> names);
}
