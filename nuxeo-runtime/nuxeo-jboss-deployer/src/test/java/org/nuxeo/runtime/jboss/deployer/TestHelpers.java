/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.jboss.deployer;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestHelpers {

    public static void assertPathEquals(String expected, String actual) throws Exception {
        if (File.separatorChar == '\\') { // windows
            expected = expected.replace('/', '\\');
            if (expected.startsWith("\\c:")) {
                // strip starting backslash as this is how
                // windows absolute paths with a drive are returned
                expected = expected.substring(1);
            }
            if (expected.startsWith("\\")) {
                // The drive letter depends on the current working dir
                String currentDriveRoot;
                File currentDir = new File(System.getProperty("user.dir"));
                while (currentDir.getParentFile() != null) {
                    currentDir = currentDir.getParentFile();
                }
                currentDriveRoot = currentDir.getCanonicalPath();
                expected = currentDriveRoot + expected.substring(1);
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testFileConvertor() throws Exception {
        URL url;
        url = new URL("file:/foo/bar");
        assertPathEquals("/foo/bar",
                Utils.tryGetFile(url).getAbsolutePath());
        url = new URL("file:///c:/Documents and Settings/test");
        assertPathEquals("/c:/Documents and Settings/test",
                Utils.tryGetFile(url).getAbsolutePath());
        url = new URL("file:///c:/Documents%20and%20Settings/test");
        assertPathEquals("/c:/Documents and Settings/test",
                Utils.tryGetFile(url).getAbsolutePath());
        url = new URL(
                "jar:file:/opt/jboss5/jboss-5.1.0.GA/server/default/tmp/x/nuxeo.ear!/");
        assertPathEquals(
                "/opt/jboss5/jboss-5.1.0.GA/server/default/tmp/x/nuxeo.ear",
                Utils.tryGetFile(url).getAbsolutePath());
        url = new URL(
                "jar:file:/opt/jboss5/jboss-5.1.0.GA/my server/default/tmp/x/nuxeo.ear!/");
        assertPathEquals(
                "/opt/jboss5/jboss-5.1.0.GA/my server/default/tmp/x/nuxeo.ear",
                Utils.tryGetFile(url).getAbsolutePath());
        url = new URL(
                "jar:file:/opt/jboss5/jboss-5.1.0.GA/my%20server/default/tmp/x/nuxeo.ear!/");
        assertPathEquals(
                "/opt/jboss5/jboss-5.1.0.GA/my server/default/tmp/x/nuxeo.ear",
                Utils.tryGetFile(url).getAbsolutePath());
    }

}
