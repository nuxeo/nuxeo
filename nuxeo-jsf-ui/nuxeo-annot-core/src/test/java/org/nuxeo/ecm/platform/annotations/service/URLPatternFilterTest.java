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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class URLPatternFilterTest {

    private static URLPatternFilter filter1 = new URLPatternFilter(true,
            Collections.singletonList("http://foo.apache.org"), Collections.singletonList("http://.*apache.org.*"));

    private static URLPatternFilter filter2 = new URLPatternFilter(false,
            Collections.singletonList("http://.*apache.org.*"), Collections.singletonList("http://foo.apache.org"));

    @Test
    public void testAllow() {
        assertFalse(filter1.allow("http://nuxeo.com"));
        assertTrue(filter1.allow("http://apache.org"));
        assertFalse(filter1.allow("http://foo.apache.org"));
        assertFalse(filter2.allow("http://apache.org"));
        assertTrue(filter2.allow("http://foo.apache.org"));
        assertTrue(filter2.allow("http://nuxeo.com"));
    }

}
