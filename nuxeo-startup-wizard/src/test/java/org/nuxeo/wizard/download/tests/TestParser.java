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

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.wizard.download.DownloadDescriptorParser;
import org.nuxeo.wizard.download.DownloadPackage;
import org.nuxeo.wizard.download.DownloadablePackageOption;
import org.nuxeo.wizard.download.DownloadablePackageOptions;

public class TestParser {

    @Test
    public void testParser1() {

        InputStream stream = this.getClass().getResourceAsStream(
                "/packages_old.xml");
        assertNotNull(stream);

        DownloadablePackageOptions pkgs = DownloadDescriptorParser.parsePackages(stream);
        assertNotNull(pkgs);
        assertEquals(3, pkgs.size());

        assertEquals(1, pkgs.get(0).getChildrenPackages().size()); // CAP => DAM
        assertEquals(2, pkgs.get(1).getChildrenPackages().size()); // DM => DAM,
                                                                   // Collab
        assertEquals(0, pkgs.get(2).getChildrenPackages().size()); // CMF =>
                                                                   // Nothing

        assertEquals(true, pkgs.get(0).isExclusive());
        assertEquals(true, pkgs.get(1).isExclusive());
        assertEquals(true, pkgs.get(2).isExclusive());
        assertEquals(false,
                pkgs.get(1).getChildrenPackages().get(0).isExclusive());
        assertEquals(false,
                pkgs.get(1).getChildrenPackages().get(1).isExclusive());

        assertNull(pkgs.get(0).getPackage()); // Fake package
        assertNotNull(pkgs.get(1).getPackage());
        assertNotNull(pkgs.get(1).getPackage().getBaseUrl());
        assertNotNull(pkgs.get(1).getChildrenPackages().get(0).getPackage());
        assertNotNull(pkgs.get(1).getChildrenPackages().get(0).getPackage().getBaseUrl());

        // test selection DM + DAM + Collab
        List<String> ids = new ArrayList<String>();
        ids.add(pkgs.get(1).getId()); // DM
        ids.add(pkgs.get(1).getChildrenPackages().get(0).getId()); // DAM
        ids.add(pkgs.get(1).getChildrenPackages().get(1).getId()); // Collab

        pkgs.select(ids);
        List<DownloadPackage> pkg4Download = pkgs.getPkg4Download();
        // System.out.println(pkg4Download.toString());
        assertEquals(4, pkg4Download.size()); // CAP + DM + DAM + COLLAB

        // test selection CAP + DAM
        ids = new ArrayList<String>();
        ids.add(pkgs.get(0).getId()); // CAP
        ids.add(pkgs.get(0).getChildrenPackages().get(0).getId()); // DAM

        pkgs.select(ids);
        pkg4Download = pkgs.getPkg4Download();
        // System.out.println(pkg4Download.toString());
        assertEquals(2, pkg4Download.size()); // CAP + DAM

    }
    @Test
    public void testParserAndSelection() {

        InputStream stream = this.getClass().getResourceAsStream(
                "/packages.xml");
        assertNotNull(stream);

        DownloadablePackageOptions pkgs = DownloadDescriptorParser.parsePackages(stream);
        assertNotNull(pkgs);
        assertEquals(1, pkgs.size());

        DownloadablePackageOption root = pkgs.get(0);
        assertEquals("nuxeo-cap", root.getPackage().getId());

        assertEquals(3, root.getChildrenPackages().size()); // DAM / DM / CMF

        // System.out.println(pkgs.asJson());

        // Check selections
        List<String> selectedIds = new ArrayList<String>();
        selectedIds.add("nuxeo-social-collaboration");

        pkgs.select(selectedIds);
        assertEquals(4, pkgs.getPkg4Download().size()); // Collab / DM /
                                                        // nuxeo-content-browser
                                                        // / CAP
        assertEquals("nuxeo-social-collaboration", pkgs.getPkg4Download().get(0).getId());
        assertEquals("SC", pkgs.getPkg4Download().get(0).getShortLabel());
        assertEquals("nuxeo-dm", pkgs.getPkg4Download().get(1).getId());
        assertEquals("DM", pkgs.getPkg4Download().get(1).getShortLabel());
        assertEquals("nuxeo-content-browser",
                pkgs.getPkg4Download().get(2).getId());
        assertEquals("nuxeo-cap", pkgs.getPkg4Download().get(3).getId());
        assertEquals("CAP", pkgs.getPkg4Download().get(3).getShortLabel());

        selectedIds.clear();
        selectedIds.add("nuxeo-cmf");
        pkgs.select(selectedIds);
        assertEquals(3, pkgs.getPkg4Download().size()); // CMF /
                                                        // nuxeo-content-browser-cmf
                                                        // / CAP
        assertEquals("nuxeo-cmf", pkgs.getPkg4Download().get(0).getId());
        assertEquals("nuxeo-content-browser-cmf",
                pkgs.getPkg4Download().get(1).getId());
        assertEquals("nuxeo-cap", pkgs.getPkg4Download().get(2).getId());

        selectedIds.clear();
        selectedIds.add("nuxeo-cmf");
        selectedIds.add("nuxeo-dm");
        pkgs.select(selectedIds);
        assertEquals(3, pkgs.getPkg4Download().size()); // DM /
                                                        // nuxeo-content-browser
                                                        // / CAP
        assertEquals("nuxeo-dm", pkgs.getPkg4Download().get(0).getId());
        assertEquals("nuxeo-content-browser",
                pkgs.getPkg4Download().get(1).getId());
        assertEquals("nuxeo-cap", pkgs.getPkg4Download().get(2).getId());

    }

}
