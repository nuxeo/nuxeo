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

package org.nuxeo.connect.download.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.data.PackageDescriptor;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.update.Version;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;


@RunWith(FeaturesRunner.class)
@Features(DownloadFeature.class)
@Jetty(port = 8082)
public class TestDownloader {

    @Test
    public void testSimpleDownload() throws Exception {
        ConnectDownloadManager cdm = NuxeoConnectClient.getDownloadManager();
        Assert.assertNotNull(cdm);

        int nbDownloads = 5;
        int maxLoop = 40;

        List<PackageDescriptor> pkgToDownload = new ArrayList<PackageDescriptor>();
        for (int i = 0; i < nbDownloads; i++) {
            PackageDescriptor pkg = new PackageDescriptor();
            pkg.setSourceUrl("test" + i);
            pkg.setName("FakePackage-" + i);
            pkg.setVersion(new Version(1));
            pkgToDownload.add(pkg);
        }

        List<DownloadingPackage> downloads = new ArrayList<DownloadingPackage>();

        for (PackageDescriptor pkg : pkgToDownload) {
            DownloadingPackage lpkg = cdm.storeDownloadedBundle(pkg);
            Assert.assertNotNull(lpkg);
            downloads.add(lpkg);
            Thread.sleep(100);
        }

        boolean downloadInProgress = true;

        int nbLoop = 0;
        while (downloadInProgress) {
            downloadInProgress = false;
            for (DownloadingPackage pkg : downloads) {
                if (pkg.isCompleted()) {
                    System.out.print(pkg.getId() + ":terminated  - ");
                } else {
                    downloadInProgress = true;
                    System.out.print(pkg.getId() + ":in progress - ");
                }
            }
            System.out.println();
            nbLoop++;
            if (nbLoop > maxLoop) {
                throw new RuntimeException("Download is stuck");
            }
            Thread.sleep(500);
        }
    }

}
