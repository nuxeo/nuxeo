/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;

/**
 * @author Alexandre Russel
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
                return url.substring(0, url.indexOf(NUXEO_ANNOTATIONS) + NUXEO_ANNOTATIONS.length());
            } else {
                return url.substring(0, url.indexOf("nuxeo") + "nuxeo".length());
            }
        } catch (MalformedURLException e) {
            return null; // urn
        }
    }

    public URI getSearchURI(URI uri) {
        return uri;
    }

    public URI translateFromGraphURI(URI uri, String baseUrl) {
        if (uri.toString().startsWith("urn:annotation:")) {
            String annId = uri.toString().substring(uri.toString().lastIndexOf(":") + 1);
            try {
                return new URI(baseUrl + annId);
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        }
        return uri;
    }

    public URI translateToGraphURI(URI uri) {
        String path = uri.getPath();
        if (uri.toString().contains(NUXEO_ANNOTATIONS)) {
            try {
                return new URI("urn:annotation:" + path.substring(path.lastIndexOf("/") + 1));
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        } else {
            return uri;
        }
    }
}
