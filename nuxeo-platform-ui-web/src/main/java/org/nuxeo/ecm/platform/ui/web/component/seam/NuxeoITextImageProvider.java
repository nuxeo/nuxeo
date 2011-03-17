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
 * Nuxeo image provider handling base url and authentication propagation when
 * resolving resources on server.
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
    public Image getImage(String src, HashMap h, ChainedProperties cprops,
            DocListener doc) {
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
        src = uriPath + ";jsessionid="
                + DocumentModelFunctions.extractJSessionId(request);
        URI uri = URI.create(src);
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
