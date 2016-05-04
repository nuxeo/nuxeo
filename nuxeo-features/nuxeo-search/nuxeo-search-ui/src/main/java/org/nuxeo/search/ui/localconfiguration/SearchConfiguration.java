/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.search.ui.localconfiguration;

import java.util.List;

import org.nuxeo.ecm.core.api.localconfiguration.LocalConfiguration;

/**
 * @since 6.0
 */
public interface SearchConfiguration extends LocalConfiguration<SearchConfiguration> {

    /**
     * Return a list of content views name that are denied with the local configuration
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
