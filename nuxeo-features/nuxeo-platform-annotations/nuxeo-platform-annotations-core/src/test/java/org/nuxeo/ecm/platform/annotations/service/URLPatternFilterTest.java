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

import java.util.Collections;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class URLPatternFilterTest {

    private static URLPatternFilter filter1 = new URLPatternFilter(true,
            Collections.singletonList("http://foo.apache.org"),
            Collections.singletonList("http://.*apache.org.*"));

    private static URLPatternFilter filter2 = new URLPatternFilter(false,
            Collections.singletonList("http://.*apache.org.*"),
            Collections.singletonList("http://foo.apache.org"));

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
