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

import java.net.URI;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.annotations.api.UriResolver;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class DefaultUriResolverTest extends TestCase {

    private static final String baseUrl = "http://myexemple.com/nuxeo/Annotations/";

    private static final String annId = "3ACF6D754";

    private static final String annotationUrl = baseUrl + annId;

    private static final String annotationUrn = "urn:annotation:" + annId;

    private final UriResolver resolver = new DefaultUriResolver();

    public void testTranslateToGraphUri() throws Exception {
        URI result = resolver.translateToGraphURI(new URI(annotationUrl));
        assertEquals(annotationUrn, result.toString());
    }

    public void testTranslateFromGraphUri() throws Exception {
        URI result = resolver.translateFromGraphURI(new URI(annotationUrn),
                baseUrl);
        assertEquals(annotationUrl, result.toString());
    }

    public void testGetBaseUrl() throws Exception {
        assertEquals(baseUrl, resolver.getBaseUrl(new URI(annotationUrl)));
    }

}
