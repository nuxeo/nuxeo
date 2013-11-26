/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;

/**
 * @author Alexandre Russel
 *
 */
public class DefaultUriResolver implements UriResolver {
    private static final String NUXEO_ANNOTATIONS = "nuxeo/Annotations/";

    public String getBaseUrl(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            String url = uri.toURL().toString();
            if (url.contains(NUXEO_ANNOTATIONS)) {
                return url.substring(0, url.indexOf(NUXEO_ANNOTATIONS)
                        + NUXEO_ANNOTATIONS.length());
            } else {
                return url.substring(0, url.indexOf("nuxeo") + "nuxeo".length());
            }
        } catch (MalformedURLException e) {
            return null; // urn
        }
    }

    public List<URI> getSearchURI(URI uri) throws AnnotationException {
        return Collections.singletonList(uri);
    }

    public URI translateFromGraphURI(URI uri, String baseUrl)
            throws AnnotationException {
        if (uri.toString().startsWith("urn:annotation:")) {
            String annId = uri.toString().substring(
                    uri.toString().lastIndexOf(":") + 1);
            try {
                return new URI(baseUrl + annId);
            } catch (URISyntaxException e) {
                throw new AnnotationException(e);
            }
        }
        return uri;
    }

    public URI translateToGraphURI(URI uri) throws AnnotationException {
        String path = uri.getPath();
        if (uri.toString().contains(NUXEO_ANNOTATIONS)) {
            try {
                return new URI("urn:annotation:"
                        + path.substring(path.lastIndexOf("/") + 1));
            } catch (URISyntaxException e) {
                throw new AnnotationException(e);
            }
        } else {
            return uri;
        }
    }
}
