/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.testing;

import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.functionaltests.drivers.FirefoxDriverProvider;
import org.nuxeo.functionaltests.proxy.ProxyManager;
import org.nuxeo.runtime.api.Framework;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Abstract explorer test with download FF features and associated helpers.
 *
 * @since 11.2
 */
public abstract class AbstractExplorerDownloadTest extends AbstractExplorerTest {

    public static File downloadDir;

    public static String SAMPLE_BUNDLE_GROUP = "apidoc";

    /**
     * Updates the firefox profile to ease up testing of downloaded distribution.
     * <p>
     * See NXP-20646 for download solution.
     */
    @BeforeClass
    public static void initFirefoxDriver() throws Exception {
        assumeTrue(driver instanceof FirefoxDriver);

        // quit existing: will recreate one
        quitDriver();

        proxyManager = new ProxyManager();
        Proxy proxy = proxyManager.startProxy();
        if (proxy != null) {
            proxy.setNoProxy("");
        }
        DesiredCapabilities dc = DesiredCapabilities.firefox();
        dc.setCapability(CapabilityType.PROXY, proxy);
        FirefoxProfile profile = FirefoxDriverProvider.getProfile();
        JavaScriptError.addExtension(profile);

        // specific profile part
        downloadDir = Framework.createTempDirectory("webdriver-explorer-admin").toFile();
        profile.setPreference("browser.download.dir", downloadDir.toString());
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/zip");

        dc.setCapability(FirefoxDriver.PROFILE, profile);
        driver = new FirefoxDriver(dc);
    }

    @AfterClass
    public static void cleanupDownloadDir() throws Exception {
        FileUtils.deleteDirectory(downloadDir);
    }

    protected static File createSampleZip(boolean addMarker) throws IOException {
        String sourceDirPath = "data/sample_export";
        File zip = new File(downloadDir, "distrib-apidoc.zip");
        FileUtils.deleteQuietly(zip);
        Path p = Files.createFile(Paths.get(zip.getPath()));
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
            if (addMarker) {
                ZipEntry zipEntry = new ZipEntry(".nuxeo-archive");
                zs.putNextEntry(zipEntry);
                zs.closeEntry();
            }
            // read paths from reference file as NuxeoArchiveReader requires a given order and extra info
            Path epath = Paths.get(sourceDirPath, "entries.txt");
            List<String> lines;
            try (InputStream stream = AbstractExplorerTest.getReferenceStream(epath)) {
                lines =IOUtils.readLines(stream, StandardCharsets.UTF_8);
            }
            for (Iterator<String> lineIter = lines.iterator(); lineIter.hasNext();) {
                String path = lineIter.next();
                if (StringUtils.isEmpty(path)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(path);
                if (path.endsWith("/")) {
                    // directory entry case
                    entry.setExtra(new DWord(Integer.valueOf(lineIter.next())).getBytes());
                    zs.putNextEntry(entry);
                } else {
                    zs.putNextEntry(entry);
                    Path ppath = Paths.get(sourceDirPath, path);
                    try (InputStream stream = getReferenceStream(ppath)) {
                        IOUtils.copy(stream, zs);
                    }
                }
                zs.closeEntry();
            }
        }
        return zip;
    }

}
