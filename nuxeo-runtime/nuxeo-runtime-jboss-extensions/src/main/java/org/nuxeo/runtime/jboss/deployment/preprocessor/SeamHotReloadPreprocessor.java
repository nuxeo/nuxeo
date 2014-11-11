/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.runtime.jboss.deployment.preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.ZipUtils;

/**
 * Proprocessor for Seam Jars
 *
 * Extract reloadable Seam Components to put them into Seam Hot Reload path
 *
 * @author tiry
 */
public class SeamHotReloadPreprocessor {

    // place where seams looks for hot reloadable classes
    public static final String SEAM_HOT_RELOAD_DIR = "nuxeo.war/WEB-INF/dev";
    // directory where patched jars are stored and deployed
    public static final String SEAM_PATCHED_JARS_DIRECTORY = "seamHotReloadableJars";
    // flag file used to detect Seam jars
    public static final String SEAMJAR_MARKER = "seam.properties";
    // property file used to enable/disable seam hot reload preprocessing
    public static final String SEAM_HOT_RELOAD_GLOBAL_CONFIG = "config/seam-debug.properties";
    // global system property name to indicate to Nuxeo if Seam debug is enables
    public static final String SEAM_HOT_RELOAD_SYSTEM_PROP = "org.nuxeo.seam.debug";

    protected final Logger log;

    protected final File earDirectory;

    protected final List<String> globalyDefinedReloadableClasses = new ArrayList<String>();

    protected boolean enabled = false;

    public SeamHotReloadPreprocessor(File earDirectory, Logger log) {
        this.log = log;
        this.earDirectory = earDirectory;

        File seamDebugFile = new File(earDirectory,
                SEAM_HOT_RELOAD_GLOBAL_CONFIG);
        if (seamDebugFile.exists()) {
            enabled = true;
            try {
                InputStream seamDebugPropertyStream = new FileInputStream(
                        seamDebugFile);
                Properties props = new Properties();
                props.load(seamDebugPropertyStream);
                seamDebugPropertyStream.close();
                Enumeration<?> keys = props.propertyNames();
                while (keys.hasMoreElements()) {
                    String fcn = (String) keys.nextElement();
                    globalyDefinedReloadableClasses.add(fcn);
                }
                log.info("Nuxeo's Seam HotReload Preprocessor is enabled");
                System.setProperty(SEAM_HOT_RELOAD_SYSTEM_PROP, "true");
            } catch (Exception e) {
                log.error("Error duing parsing of "
                        + SEAM_HOT_RELOAD_GLOBAL_CONFIG, e);
            }
        } else {
            log.info("Nuxeo's Seam HotReload Preprocessus is not enabled");
            enabled = false;

        }
    }

    protected File getJar(DeploymentInfo di) throws DeploymentException {
        try {
            String url = di.url.toString();
            url = url.replace(" ", "%20");
            return new File(new URI(url));
        } catch (Exception e) {
            throw new DeploymentException("Cannot get jar: " + di.shortName, e);
        }
    }

    protected List<String> getHotReloadableSeamClasses(File jar) {
        List<String> fullClassNames = null;
        try {
            InputStream seamProperties = ZipUtils.getEntryContentAsStream(jar,
                    SEAMJAR_MARKER);

            if (seamProperties != null) {
                Properties props = new Properties();
                props.load(seamProperties);
                seamProperties.close();
                fullClassNames = new ArrayList<String>();
                Enumeration<?> keys = props.propertyNames();
                while (keys.hasMoreElements()) {
                    String fcn = (String) keys.nextElement();
                    fullClassNames.add(fcn);
                }
                fullClassNames.addAll(globalyDefinedReloadableClasses);
            }
        } catch (Exception e) {
            if (jar.getName().endsWith("jar")) {
                log.warn("Seam dePloyer can not scan archive "
                        + jar.getAbsolutePath());
            }
        }
        return fullClassNames;
    }

    public void doProcess(List<DeploymentInfo> subDeployments) throws Exception {
        for (DeploymentInfo di : subDeployments) {
            File jar = getJar(di);
            List<String> classNames = getHotReloadableSeamClasses(jar);
            if (classNames != null && classNames.size() > 0) {
                log.info("SeamHotReloadPreprocess found Seam archive in "
                        + jar.getAbsolutePath());
                splitSeamJar(di, jar, classNames);
            }
        }
    }

    protected String getSeamHotReloadDir() {

        Path path = new Path(earDirectory.getAbsolutePath());
        path = path.append(SEAM_HOT_RELOAD_DIR);
        /*
         * path = path.append("nuxeo.war"); path = path.append("WEB-INF"); path =
         * path.append("dev2");
         */

        File seamHRD = new File(path.toString());
        if (!seamHRD.exists()) {
            seamHRD.mkdirs();
        }
        return seamHRD.getAbsolutePath();
    }

    public void initSpecialDirectory() {
        String dirPath = getDirForPatchedSeamJars();
        File dir = new File(dirPath);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }

    public boolean isSeamHotReloadEnabled() {
        return enabled;
    }

    protected String getDirForPatchedSeamJars() {

        Path path = new Path(earDirectory.getAbsolutePath());
        path = path.append(SEAM_PATCHED_JARS_DIRECTORY);

        File seamPJD = new File(path.toString());
        if (!seamPJD.exists()) {
            if (isSeamHotReloadEnabled()) {
                seamPJD.mkdirs();
            }
        }
        return seamPJD.getAbsolutePath();
    }

    protected void splitSeamJar(DeploymentInfo di, File originalJar,
            List<String> classNames) throws Exception {

        String patchedJarPath = new Path(getDirForPatchedSeamJars()).append(
                originalJar.getName()).toString();
        File patchedJar = new File(patchedJarPath);

        List<String> jarEntriesToExtract = new ArrayList<String>();
        for (String cn : classNames) {
            String path = cn.replaceAll("\\.", "/") + ".class";
            jarEntriesToExtract.add(path);
        }

        ZipFile inputZip = new ZipFile(originalJar);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
                patchedJar));

        Enumeration<ZipEntry> inputZipEntries = (Enumeration<ZipEntry>) inputZip
                .entries();
        while (inputZipEntries.hasMoreElements()) {
            ZipEntry ze = inputZipEntries.nextElement();
            if (jarEntriesToExtract.contains(ze.getName())) {
                // extract entry to HotReload location
                String path = ze.getName();
                String dstPath = new Path(getSeamHotReloadDir()).append(path)
                        .toString();
                String dstFolderPath = new Path(dstPath).removeLastSegments(1)
                        .toString();
                File dstFolder = new File(dstFolderPath);
                if (!dstFolder.exists()) {
                    dstFolder.mkdirs();
                }
                File dst = new File(dstPath);

                FileUtils.copyToFile(inputZip.getInputStream(ze), dst);
            } else {
                // copy to the modified jar non reloadable resources
                out.putNextEntry(ze);
                InputStream srcEntry = inputZip.getInputStream(ze);
                byte[] buf = new byte[1024];
                int len;
                while ((len = srcEntry.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                srcEntry.close();
            }
        }
        out.close();

        // switch jar in deployment listing
        updateDeploymentInfo(di, patchedJar);
    }

    protected void updateDeploymentInfo(DeploymentInfo di, File newJar)
            throws MalformedURLException {
        di.url = newJar.toURL();
        di.watch = newJar.toURL();
    }

}
