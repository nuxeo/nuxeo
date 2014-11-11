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
package org.nuxeo.connect.update.standalone.registry;

import org.junit.Test;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestVersions extends SharedFilesTest {

    /**
     * Test with downgrade off
     *
     * @throws Exception
     */
    @Test
    public void testUpgrade() throws Exception {
        createFakeBundles();

        Pkg1 pkg1 = new Pkg1();
        pkg1.setAllowDowngrade(false);
        Pkg2 pkg2 = new Pkg2();

        ensureBaseVersion();

        // +pkg1, +pkg2, -pkg1, -pkg2
        pkg1.install();
        ensurePkg1();

        pkg2.install();
        ensurePkg12();

        pkg1.uninstall();
        ensurePkg2();

        pkg2.uninstall();
        ensureBaseVersion();

        // +pkg1, +pkg2, -pkg2, -pkg1
        pkg1.install();
        ensurePkg1();

        pkg2.install();
        ensurePkg12();

        pkg2.uninstall();
        ensurePkg1();

        pkg1.uninstall();
        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg2, -pkg1
        pkg2.install();
        ensurePkg2();

        pkg1.install();
        ensurePkg21();

        pkg2.uninstall();
        ensurePkg1();
        // ensurePkg1AfterBlockingDowngrade();

        pkg1.uninstall();
        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg1, -pkg2
        pkg2.install();
        ensurePkg2();

        pkg1.install();
        ensurePkg21();

        pkg1.uninstall();
        ensurePkg2();

        pkg2.uninstall();
        ensureBaseVersion();
    }

}
