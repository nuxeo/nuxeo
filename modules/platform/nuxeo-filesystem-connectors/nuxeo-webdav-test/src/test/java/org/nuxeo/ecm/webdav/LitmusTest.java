/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
        String[] envp = { "TESTS=basic copymove" };
        Process p = Runtime.getRuntime().exec("litmus -k " + getRootUri(), envp);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = reader.readLine()) != null) {
            System.out.println(s);
            System.out.flush();
            if (s.startsWith("<- summary for ")) {
                assertTrue(s.contains(" 0 failed."));
            }
        }
        assertEquals(0L, p.waitFor());
    }

}
