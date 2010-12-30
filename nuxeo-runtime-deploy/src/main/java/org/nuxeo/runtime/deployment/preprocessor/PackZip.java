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
 *     bstefanescu, jcarsique
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

import org.nuxeo.common.utils.FileUtils;
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
 *
 */
public class PackZip {

    protected File nuxeoEar;

    protected File deployerJar;

    protected File deployDir;

    protected File jbossLib;

    protected File dsFile;

    protected File target;

    public PackZip(File nuxeoEar, File target) {
        if (!nuxeoEar.isDirectory()) {
            throw new IllegalArgumentException(
                    "Invalid build - no exploded nuxeo.ear found at "
                            + nuxeoEar.getAbsolutePath());
        }
        if (!target.isDirectory()) {
            throw new IllegalArgumentException(
                    "Invalid configuration - no target directory found at "
                            + nuxeoEar.getAbsolutePath());
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
            if (name.startsWith("nuxeo-jboss-deployer")
                    && name.endsWith(".jar")) {
                deployerJar = new File(deployers, name);
            }
        }
        if (deployerJar == null) {
            throw new IllegalArgumentException(
                    "Invalid build - no nuxeo jboss deployer JAR found in deployers directory");
        }
    }

    public void execute() throws ConfigurationException, IOException,
            ParserConfigurationException, SAXException {
        // configure from templates
        new ConfigurationGenerator().run();
        // run preprocessor
        runPreprocessor();
        // move non ejb jars to nuxeo.ear/lib
        moveNonEjbsToLib(nuxeoEar);
        // replace nuxeo-structure.xml with nuxeo-structure-zip.xml
        replaceStructureFile();
        // move libs in jboss/lib to nuxeo.ear/lib
        moveJarsFromJbossLib();
        // move nuxeo jboss deployer to nuxeo.ear/lib
        deployerJar.renameTo(new File(nuxeoEar, "lib/" + deployerJar.getName()));
        // zip the ear into target directory
        ZipUtils.zip(nuxeoEar.listFiles(), new File(target, "nuxeo.ear"));
        // copy nuxeo-ds.xml to target dir
        FileUtils.copy(dsFile, target);
    }

    protected void runPreprocessor() {
        DeploymentPreprocessor.main(new String[] { nuxeoEar.getAbsolutePath() });
    }

    protected void replaceStructureFile() {
        File oldf = new File(nuxeoEar, "META-INF/nuxeo-structure.xml");
        File newf = new File(nuxeoEar, "META-INF/nuxeo-structure-zip.xml");
        newf.renameTo(oldf);
    }

    protected void moveJarsFromJbossLib() {
        // TODO do we really need that?
    }

    protected void moveNonEjbsToLib(File wd)
            throws ParserConfigurationException, SAXException, IOException {
        File file = new File(wd, "META-INF/application.xml");
        if (!file.isFile()) {
            System.err.println("You should run this tool from a preprocessed nuxeo.ear folder");
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream in = new FileInputStream(file);
        Document doc = docBuilder.parse(in);
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("module");
        Collection<String> paths = new ArrayList<String>();
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
            System.out.println("Move EAR module " + path + " to "
                    + ejbs.getName());
            File f = new File(wd, path);
            f.renameTo(new File(ejbs, f.getName()));
        }
        File lib = new File(wd, "lib");
        File[] files = new File(wd, "bundles").listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("Move POJO bundle " + f.getName()
                        + " to lib");
                f.renameTo(new File(lib, f.getName()));
            }
        }
        File bundles = new File(wd, "bundles");
        files = ejbs.listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("Move back EAR module " + f.getName()
                        + " to bundles");
                f.renameTo(new File(bundles, f.getName()));
            }
        }
    }

    protected static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void main(String[] args) throws IOException,
            ConfigurationException, ParserConfigurationException, SAXException {
        if (args.length != 2) {
            fail("Usage: PackZip nuxeo_ear_directory target_directory");
            System.err.println();
        }
        String v = args[0];
        File ear = v.startsWith("/") ? new File(v) : new File(".", v);
        if (!ear.isDirectory()) {
            fail("Invalid build - no exploded nuxeo.ear found at "
                    + ear.getAbsolutePath());
        }
        v = args[1];
        File target = v.startsWith("/") ? new File(v) : new File(".", v);
        ear = ear.getCanonicalFile();
        target = target.getCanonicalFile();
        if (target.exists()) {
            FileUtils.deleteTree(target);
        }
        target.mkdirs();
        if (!target.isDirectory()) {
            fail("Invalid target directory: " + v
                    + ". Not a directory or directory could not be created");
        }

        System.out.println("Packing nuxeo.ear at " + ear.getAbsolutePath()
                + " into " + target.getAbsolutePath());

        new PackZip(ear, target).execute();
    }
}
