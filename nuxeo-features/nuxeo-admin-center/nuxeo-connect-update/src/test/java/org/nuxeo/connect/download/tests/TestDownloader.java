/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.connect.download.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.DownloadingPackage;
import org.nuxeo.connect.data.PackageDescriptor;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.downloads.LocalDownloadingPackage;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.Version;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

@RunWith(FeaturesRunner.class)
@Features(DownloadFeature.class)
public class TestDownloader {

    protected static final Log log = LogFactory.getLog(TestDownloader.class);

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Before
    public void setUpPort() {
        int port = servletContainerFeature.getPort();
        ConnectUrlConfig.setTestPort(port);
    }

    @Test
    public void testSimpleDownload() throws Exception {
        ConnectDownloadManager cdm = NuxeoConnectClient.getDownloadManager();
        assertNotNull(cdm);

        int nbDownloads = 5;
        int maxLoop = 40;

        List<PackageDescriptor> pkgToDownload = new ArrayList<>();
        for (int i = 0; i < nbDownloads; i++) {
            PackageDescriptor pkg = new PackageDescriptor();
            pkg.setSourceUrl("test" + i);
            pkg.setName("FakePackage-" + i);
            pkg.setVersion(new Version(1));
            pkg.setPackageState(PackageState.UNKNOWN);
            pkgToDownload.add(pkg);
        }

        List<DownloadingPackage> downloads = new ArrayList<>();

        for (PackageDescriptor pkg : pkgToDownload) {
            DownloadingPackage lpkg = cdm.storeDownloadedBundle(pkg);
            assertNotNull(lpkg);
            downloads.add(lpkg);
            Thread.sleep(100);
        }

        boolean downloadInProgress = true;

        int nbLoop = 0;
        while (downloadInProgress) {
            downloadInProgress = false;
            for (DownloadingPackage pkg : downloads) {
                if (pkg.isCompleted()) {
                    assertNull(pkg.getErrorMessage());
                    File file = ((LocalDownloadingPackage) pkg).getFile();
                    String content = FileUtils.readFileToString(file, UTF_8);
                    assertEquals("Fake package\nFake line\nFake line\nFake line\nFake line\n" //
                            + "Fake line\nFake line\nFake line\nFake line\nFake line\n", //
                            content);
                    log.info(pkg.getId() + ":complete  - ");
                } else {
                    downloadInProgress = true;
                    log.info(pkg.getId() + ":in progress - ");
                }
            }
            nbLoop++;
            if (nbLoop > maxLoop) {
                throw new RuntimeException("Download is stuck");
            }
            Thread.sleep(500);
        }
    }

}
