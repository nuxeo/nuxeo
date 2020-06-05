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
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.lang3.SystemUtils;
import org.junit.ComparisonFailure;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageData;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.Version;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class with utility methods.O
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeSnaphotFeature.class, MockitoFeature.class })
public abstract class AbstractApidocTest {

    // helper for quicker update when running tests locally
    public static final boolean UPDATE_REFERENCE_FILES_ON_FAILURE = false;

    @RuntimeService
    @Mock
    protected PackageManager packageManager;

    @RuntimeService
    @Mock
    protected PackageUpdateService packageUpdateService;

    public void mockPackageServices() throws PackageException, IOException {
        String mockPackageName = "platform-explorer-mock";
        String mockPackageVersion = "1.0.1";
        String mockPackageId = mockPackageName + "-" + mockPackageVersion;
        DownloadablePackage mockPackage = mock(DownloadablePackage.class);
        when(mockPackage.getId()).thenReturn(mockPackageId);
        when(packageManager.listInstalledPackages()).thenReturn(List.of(mockPackage));
        LocalPackage mockLocalPackage = mock(LocalPackage.class);
        when(packageUpdateService.getPackage(mockPackageId)).thenReturn(mockLocalPackage);
        when(mockLocalPackage.getId()).thenReturn(mockPackageId);
        when(mockLocalPackage.getName()).thenReturn(mockPackageName);
        when(mockLocalPackage.getVersion()).thenReturn(new Version(mockPackageVersion));
        when(mockLocalPackage.getTitle()).thenReturn("Platform Explorer Mock");
        when(mockLocalPackage.getType()).thenReturn(PackageType.ADDON);
        // mock dep
        when(mockLocalPackage.getDependencies()).thenReturn(
                new PackageDependency[] { new PackageDependency("platform-explorer-base") });
        when(mockLocalPackage.getOptionalDependencies()).thenReturn(new PackageDependency[0]);
        when(mockLocalPackage.getConflicts()).thenReturn(new PackageDependency[0]);
        PackageData mockData = mock(PackageData.class);
        when(mockLocalPackage.getData()).thenReturn(mockData);

        Path sourceDir = Paths.get(getReferencePath("apidoc_package"));
        Path targetPackageDir = Paths.get(FeaturesRunner.getBuildDirectory(), "mock_apidoc_package");
        Path targetJarDir = Paths.get(targetPackageDir.toString(), "install", "bundles");
        new File(targetJarDir.toString()).mkdirs();
        for (String jar : List.of("nuxeo-apidoc-core-11.1-SNAPSHOT", "nuxeo-apidoc-repo-11.1-SNAPSHOT")) {
            createMockJar(Paths.get(sourceDir.toString(), jar, "MANIFEST.MF"),
                    Paths.get(targetJarDir.toString(), jar + ".jar"));
        }
        when(mockData.getRoot()).thenReturn(new File(targetPackageDir.toString()));
    }

    protected void createMockJar(Path manifestPath, Path target) throws IOException {
        Manifest manifest = new Manifest(new FileInputStream(new File(manifestPath.toString())));
        JarOutputStream targetJar = new JarOutputStream(Files.newOutputStream(target), manifest);
        targetJar.close();
    }

    protected void checkContentEquals(String path, String actualContent) throws IOException {
        checkContentEquals(path, actualContent, UPDATE_REFERENCE_FILES_ON_FAILURE, false);
    }

    protected void checkContentEquals(String path, String actualContent, boolean updateOnFailure, boolean isReference)
            throws IOException {
        String message = String.format("File '%s' content differs: ", path);
        String expectedPath = getReferencePath(path);
        String expectedContent = getReferenceContent(expectedPath);
        if (actualContent != null) {
            actualContent = actualContent.trim();
            if (SystemUtils.IS_OS_WINDOWS) {
                // replace end of lines while testing on windows
                actualContent = actualContent.replaceAll("\r?\n", "\n");
            }
        }
        try {
            assertEquals(message, expectedContent, actualContent);
        } catch (ComparisonFailure e) {
            // copy content locally to ease up updates when running tests locally
            if (updateOnFailure) {
                // ugly hack to get the actual resource file path:
                // - bin/* are for Eclipse;
                // - target/classes* for IntelliJ.
                String resourcePath = expectedPath.replace("bin/test", "src/test/resources")
                                                  .replace("bin/main", "src/main/resources")
                                                  .replace("target/test-classes", "src/test/resources")
                                                  .replace("target/classes", "src/main/resources");
                org.apache.commons.io.FileUtils.copyInputStreamToFile(
                        new ByteArrayInputStream((actualContent + "\n").getBytes()), new File(resourcePath));
            }
            if (isReference) {
                throw new AssertionError(String.format("Reference file '%s' content updated", path));
            } else {
                throw e;
            }
        }
    }

    public static String getReferencePath(String path) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (fileUrl == null) {
            throw new IllegalStateException("File not found: " + path);
        }
        return FileUtils.getFilePathFromUrl(fileUrl);
    }

    public static String getReferenceContent(String refPath) throws IOException {
        return org.apache.commons.io.FileUtils.readFileToString(new File(refPath), StandardCharsets.UTF_8).trim();
    }

}
