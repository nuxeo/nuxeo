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

package org.nuxeo.search.ui.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentPathCodec;

/**
 * Codec used for 'nxsearch' URLs.
 *
 * @since 5.9.6
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
