/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import com.lowagie.text.BadElementException;
import com.lowagie.text.DocListener;
import com.lowagie.text.Image;
import com.lowagie.text.html.simpleparser.ChainedProperties;
import com.lowagie.text.html.simpleparser.ImageProvider;

/**
 * Nuxeo image provider handling base url and authentication propagation when resolving resources on server.
 *
 * @since 5.4.2
 */
public class NuxeoITextImageProvider implements ImageProvider {

    protected final HttpServletRequest request;

    public NuxeoITextImageProvider(HttpServletRequest request) {
        super();
        this.request = request;
    }

    @Override
    public Image getImage(String src, HashMap h, ChainedProperties cprops, DocListener doc) {
        if (!src.startsWith("http")) {
            // add base url
            String base = VirtualHostHelper.getServerURL(request, false);
            if (base != null && base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            if (base != null) {
                src = base + src;
            }
        }
        // pass jsession id for authentication propagation
        String uriPath = URIUtils.getURIPath(src);
        src = uriPath + ";jsessionid=" + DocumentModelFunctions.extractJSessionId(request);
        if (!src.startsWith("http")) {
            // sanity double check
            throw new RuntimeException("Invalid source: " + src);
        }
        URI uri = URI.create(src); // NOSONAR (only HTTP/HTTPS URIs allowed)
        String uriQuery = uri.getQuery();
        if (uriQuery != null && uriQuery.length() > 0) {
            src = src + '?' + uriQuery;
        }
        try {
            return Image.getInstance(src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BadElementException e) {
            throw new RuntimeException(e);
        }
    }
}
