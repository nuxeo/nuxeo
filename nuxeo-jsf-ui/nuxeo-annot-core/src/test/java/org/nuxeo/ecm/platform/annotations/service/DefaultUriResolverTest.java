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

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class DefaultUriResolverTest {

    private static final String baseUrl = "http://myexemple.com/nuxeo/Annotations/";

    private static final String annId = "3ACF6D754";

    private static final String annotationUrl = baseUrl + annId;

    private static final String annotationUrn = "urn:annotation:" + annId;

    private final UriResolver resolver = new DefaultUriResolver();

    @Test
    public void testTranslateToGraphUri() throws Exception {
        URI result = resolver.translateToGraphURI(new URI(annotationUrl));
        assertEquals(annotationUrn, result.toString());
    }

    @Test
    public void testTranslateFromGraphUri() throws Exception {
        URI result = resolver.translateFromGraphURI(new URI(annotationUrn), baseUrl);
        assertEquals(annotationUrl, result.toString());
    }

    @Test
    public void testGetBaseUrl() throws Exception {
        assertEquals(baseUrl, resolver.getBaseUrl(new URI(annotationUrl)));
    }

}
