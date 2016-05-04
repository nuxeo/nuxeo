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

package org.nuxeo.search.ui.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentPathCodec;

/**
 * Codec used for 'nxsearch' URLs.
 *
 * @since 6.0
 */
public class SearchCodec extends DocumentPathCodec {

    private static final Log log = LogFactory.getLog(SearchCodec.class);

    public static final String PREFIX = "nxsearch";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    /**
     * Never handle document views: this codec is useless on post requests.
     */
    @Override
    public boolean handleDocumentView(DocumentView docView) {
        return false;
    }

}
