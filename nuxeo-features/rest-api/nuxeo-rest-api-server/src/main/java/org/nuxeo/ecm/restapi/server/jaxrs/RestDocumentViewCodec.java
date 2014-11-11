/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

import com.google.common.base.Joiner;

/**
 * Codec that resolve the Rest url for a document
 *
 * @since 5.7.2
 */
public class RestDocumentViewCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "site/api/v1/id";



    public RestDocumentViewCodec() {
    }

    public RestDocumentViewCodec(String prefix) {
    }

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {

            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            IdRef docRef = docLoc.getIdRef();
            if (docRef == null) {
                return null;
            }
            items.add(docRef.toString());
            String uri = Joiner.on("/").join(items);
            return URIUtils.addParametersToURIQuery(uri,
                    docView.getParameters());
        }
        return null;
    }

    /**
     * There is no document veiw for Rest codec
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        return null;
    }

}
