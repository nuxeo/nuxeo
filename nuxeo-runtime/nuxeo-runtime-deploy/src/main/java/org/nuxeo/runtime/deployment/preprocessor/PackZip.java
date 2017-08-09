/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     jcarsique
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.deployment.preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.launcher.config.ConfigurationException;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackZip {

    private static Log log = LogFactory.getLog(PackZip.class);

    public static final String ORDER_PREPROCESSING = "preprocessing";

    public static final String ORDER_PACKAGING = "packaging";

    protected File nuxeoEar;

    protected File deployerJar;

    protected File deployDir;

    protected File jbossLib;

    protected File dsFile;

    protected File target;

    public PackZip(File nuxeoEar, File target) {
        if (!nuxeoEar.isDirectory()) {
            throw new IllegalArgumentException(
                    "Invalid build - no exploded nuxeo.ear found at " + nuxeoEar.getAbsolutePath());
        }
        if (!target.isDirectory()) {
            throw new IllegalArgumentException(
                    "Invalid configuration - no target directory found at " + nuxeoEar.getAbsolutePath());
        }
        this.nuxeoEar = nuxeoEar;
        this.target = target;
        this.deployDir = nuxeoEar.getParentFile();
        this.jbossLib = new File(deployDir.getParentFile(), "lib");
        this.dsFile = new File(deployDir, "nuxeo-ds.xml");
        File deployers = new File(deployDir.getParentFile(), "deployers");
        String[] names = deployers.list();
        if (names == null) {
            throw new IllegalArgumentException(
                    "Invalid nuxeo.ear location - no nuxeo jboss deployer JAR found in deployers directory");
        }
        for (String name : names) {
            if (name.startsWith("nuxeo-jboss-deployer") && name.endsWith(".jar")) {
                deployerJar = new File(deployers, name);
            }
        }
        if (deployerJar == null) {
            throw new IllegalArgumentException(
                    "Invalid build - no nuxeo jboss deployer JAR found in deployers directory");
        }
    }

    protected void executePreprocessing() throws ConfigurationException, IOException {
        // configure from templates
        new ConfigurationGenerator().run();
        // run preprocessor
        runPreprocessor();
    }

    protected void executePackaging() throws IOException, SAXException, ParserConfigurationException {
        // move non ejb jars to nuxeo.ear/lib
        moveNonEjbsToLib(nuxeoEar);
        // replace nuxeo-structure.xml with nuxeo-structure-zip.xml
        replaceStructureFile();
        // move libs in jboss/lib to nuxeo.ear/lib
        moveJarsFromJbossLib();
        // move nuxeo jboss deployer to nuxeo.ear/lib
        FileUtils.moveFile(deployerJar, new File(nuxeoEar, "lib" + File.separator + deployerJar.getName()));
        // zip the ear into target directory
        ZipUtils.zip(nuxeoEar.listFiles(), new File(target, "nuxeo.ear"));
        // copy nuxeo-ds.xml to target dir
        FileUtils.copyFileToDirectory(dsFile, target);
    }

    public void execute(String order)
            throws ConfigurationException, IOException, ParserConfigurationException, SAXException {
        if (ORDER_PREPROCESSING.equals(order) || StringUtils.isBlank(order)) {
            executePreprocessing();
        }
        if (ORDER_PACKAGING.equals(order) || StringUtils.isBlank(order)) {
            executePackaging();
        }
        if (!(ORDER_PREPROCESSING.equals(order) || StringUtils.isBlank(order) || ORDER_PACKAGING.equals(order))) {
            fail("Order param should be " + ORDER_PREPROCESSING + " or " + ORDER_PACKAGING);
        }
    }

    protected void runPreprocessor() throws IOException {
        DeploymentPreprocessor.main(new String[] { nuxeoEar.getAbsolutePath() });
    }

    protected void replaceStructureFile() throws IOException {
        File oldf = new File(nuxeoEar, "META-INF" + File.separator + "nuxeo-structure.xml");
        File newf = new File(nuxeoEar, "META-INF" + File.separator + "nuxeo-structure-zip.xml");
        if (oldf.exists() && !FileUtils.deleteQuietly(oldf)) {
            log.warn("Cannot delete " + oldf.getName() + ", it may not replace it with the new file.");
        }
        FileUtils.moveFile(newf, oldf);
    }

    protected void moveJarsFromJbossLib() {
    }

    protected void moveNonEjbsToLib(File wd) throws ParserConfigurationException, SAXException, IOException {
        File file = new File(wd, "META-INF" + File.separator + "application.xml");
        if (!file.isFile()) {
            log.error("You should run this tool from a preprocessed nuxeo.ear folder");
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream in = new FileInputStream(file);
        Document doc = docBuilder.parse(in);
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("module");
        Collection<String> paths = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            Node n = el.getFirstChild();
            while (n != null) {
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element mtype = ((Element) n);
                    String type = n.getNodeName().toLowerCase();
                    if (!"web".equals(type)) {
                        String path = mtype.getTextContent().trim();
                        paths.add(path);
                    }
                }
                n = n.getNextSibling();
            }
        }

        File ejbs = new File(wd, "tmp-ejbs");
        ejbs.mkdirs();
        for (String path : paths) {
            log.info("Move EAR module " + path + " to " + ejbs.getName());
            File f = new File(wd, path);
            if (f.getName().endsWith(".txt")) {
                continue;
            }
            FileUtils.moveToDirectory(f, ejbs, false);
        }
        File lib = new File(wd, "lib");
        File[] files = new File(wd, "bundles").listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".txt")) {
                    continue;
                }
                log.info("Move POJO bundle " + f.getName() + " to lib");
                FileUtils.moveToDirectory(f, lib, false);
            }
        }
        File bundles = new File(wd, "bundles");
        files = ejbs.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".txt")) {
                    continue;
                }
                log.info("Move back EAR module " + f.getName() + " to bundles");
                FileUtils.moveToDirectory(f, bundles, false);
            }
        }
    }

    protected static void fail(String message) {
        log.error(message);
        System.exit(1);
    }

    public static void main(String[] args)
            throws IOException, ConfigurationException, ParserConfigurationException, SAXException {
        if (args.length < 2) {
            fail("Usage: PackZip nuxeo_ear_directory target_directory [order]");
        }
        String v = args[0];
        File ear = new File(v);
        if (!ear.isDirectory()) {
            fail("Invalid build - no exploded nuxeo.ear found at " + ear.getAbsolutePath());
        }
        v = args[1];
        File target = new File(v);
        ear = ear.getCanonicalFile();
        target = target.getCanonicalFile();
        if (target.exists()) {
            FileUtils.deleteDirectory(target);
        }
        target.mkdirs();
        if (!target.isDirectory()) {
            fail("Invalid target directory: " + v + ". Not a directory or directory could not be created");
        }

        log.info("Packing nuxeo.ear at " + ear.getAbsolutePath() + " into " + target.getAbsolutePath());

        PackZip pack = new PackZip(ear, target);
        if (args.length >= 3) {
            pack.execute(args[2]);
        } else {
            pack.execute(null);
        }
    }
}
