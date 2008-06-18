/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestUtil extends TestCase {

    private static void checkDiffers(String s) {
        assertNotSame(s, Util.escapeSpecialMarkers(s));
    }

    private static void checkLoop(String s) {
        assertEquals(s, Util.unescapeSpecialMarkers(
                Util.escapeSpecialMarkers(s)));
    }

    public void testEscapeEmpty() throws Exception {
        checkDiffers(Util.EMPTY_MARKER);
        checkLoop(Util.EMPTY_MARKER);
    }
    public void testEscapeNull() throws Exception {
        checkDiffers(Util.NULL_MARKER);
        checkLoop(Util.NULL_MARKER);
    }

}
