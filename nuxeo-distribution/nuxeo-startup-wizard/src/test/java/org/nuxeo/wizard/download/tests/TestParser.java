/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.download.tests;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.wizard.download.DownloadDescriptorParser;
import org.nuxeo.wizard.download.DownloadPackage;
import org.nuxeo.wizard.download.DownloadablePackageOptions;

public class TestParser extends TestCase {


    public void testParser() {

        InputStream stream = this.getClass().getResourceAsStream("/packages.xml");
        assertNotNull(stream);

        DownloadablePackageOptions pkgs = DownloadDescriptorParser.parsePackages(stream);
        assertNotNull(pkgs);
        assertEquals(3, pkgs.size());

        assertEquals(0, pkgs.get(0).getChildrenPackages().size());
        assertEquals(2, pkgs.get(1).getChildrenPackages().size());
        assertEquals(0, pkgs.get(2).getChildrenPackages().size());

        assertEquals(true, pkgs.get(0).isExclusive());
        assertEquals(true, pkgs.get(1).isExclusive());
        assertEquals(true, pkgs.get(2).isExclusive());
        assertEquals(false, pkgs.get(1).getChildrenPackages().get(0).isExclusive());
        assertEquals(false, pkgs.get(1).getChildrenPackages().get(1).isExclusive());

        assertNotNull(pkgs.get(0).getPackage());
        assertNotNull(pkgs.get(0).getPackage().getBaseUrl());
        assertNotNull(pkgs.get(1).getPackage());
        assertNotNull(pkgs.get(1).getPackage().getBaseUrl());
        assertNotNull(pkgs.get(1).getChildrenPackages().get(0).getPackage());
        assertNotNull(pkgs.get(1).getChildrenPackages().get(0).getPackage().getBaseUrl());

        List<String> ids = new ArrayList<String>();
        ids.add(pkgs.get(1).getId());
        ids.add(pkgs.get(1).getChildrenPackages().get(1).getId());

        pkgs.select(ids);
        List<DownloadPackage> pkg4Download = pkgs.getPkg4Download();
        assertEquals(2, pkg4Download.size());



    }
}
