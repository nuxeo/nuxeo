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
package registry;

import org.junit.Test;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class TestDowngradeVersions extends SharedFilesTest {

    /**
     * Test with downgrade on
     * 
     * @throws Exception
     */
    @Test
    public void testDowngrade() throws Exception {
        createFakeBundles();

        Pkg1 pkg1 = new Pkg1();
        pkg1.setAllowDowngrade(true);
        Pkg2 pkg2 = new Pkg2();

        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg2, -pkg1
        pkg2.install(service);
        ensurePkg2();

        pkg1.install(service);
        ensurePkg21WithDowngrade();

        pkg2.uninstall(service);
        ensurePkg1();

        pkg1.uninstall(service);
        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg1, -pkg2
        pkg2.install(service);
        ensurePkg2();

        pkg1.install(service);
        ensurePkg21WithDowngrade();

        pkg1.uninstall(service);
        ensurePkg2();

        pkg2.uninstall(service);
        ensureBaseVersion();

    }

}
