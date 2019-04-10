/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webdav;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class LitmusTest extends AbstractServerTest {

    /**
     * Runs the litmus test, a third-party WebDAV compliance testing tool (must be installed separately).
     * <p>
     * Only only the "basic" and "copymove" tests are supposed to pass at this point.
     */
    @Test
    public void testWithLitmus() throws Exception {
        String[] envp = { "TESTS=basic copymove"};
        Process p = Runtime.getRuntime().exec("litmus -k " + ROOT_URI, envp);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
            System.out.flush();
            if (s.startsWith("<- summary for ")) {
                assertTrue(s.contains(" 0 failed."));
            }
        }
        assertEquals(0L, (long) p.waitFor());
    }

}
