/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Julien Carsique
 *     Florent Guillaume
 */
package org.nuxeo.runtime.deployment.preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.deployment.NuxeoStarter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Packs a Nuxeo Tomcat instance into a WAR file.
 * <p>
 * The configuration parameters must be correct before this is run, in
 * particular:
 * <ul>
 * <li>The VCS configuration must be adapted.
 * </ul>
 * The WAR will need JDBC datasources configuration to run, so the
 * {@code nuxeo.xml} file from Tomcat (for instance) must be installed
 * separately.
 * <p>
 * In addition, the JDBC libraries are needed in the global Tomcat lib.
 */
public class PackWar {

    private static final List<String> MISSING_LIBS = Arrays.asList( //
            "commons-logging", //
            "commons-lang", //
            "log4j", //
            "mail", //
            "freemarker");

    private static Log log = LogFactory.getLog(PackWar.class);

    private static final String COMMAND_PREPROCESSING = "preprocessing";

    private static final String COMMAND_PACKAGING = "packaging";

    protected File nxserver;

    protected File war;

    public PackWar(File nxserver, File war) {
        if (!nxserver.isDirectory() || !nxserver.getName().equals("nxserver")) {
            fail("No nxserver found at " + nxserver);
        }
        if (war.exists()) {
            fail("Target WAR file " + war + " already exists");
        }
        this.nxserver = nxserver;
        this.war = war;
    }

    public void execute(String command) throws Exception {
        boolean preprocessing = COMMAND_PREPROCESSING.equals(command)
                || StringUtils.isBlank(command);
        boolean packaging = COMMAND_PACKAGING.equals(command)
                || StringUtils.isBlank(command);
        if (!preprocessing && !packaging) {
            fail("Command parameter should be empty or "
                    + COMMAND_PREPROCESSING + " or " + COMMAND_PACKAGING);
        }
        if (preprocessing) {
            executePreprocessing();
        }
        if (packaging) {
            executePackaging();
        }
    }

    protected void executePreprocessing() throws Exception {
        runTemplatePreprocessor();
        runDeploymentPreprocessor();
    }

    protected void runTemplatePreprocessor() throws Exception {
        System.setProperty(ConfigurationGenerator.NUXEO_HOME,
                nxserver.getPath());
        System.setProperty(ConfigurationGenerator.NUXEO_CONF, new File(
                nxserver, "config").getPath());
        new ConfigurationGenerator().run();
    }

    protected void runDeploymentPreprocessor() throws Exception {
        DeploymentPreprocessor processor = new DeploymentPreprocessor(nxserver);
        processor.init();
        processor.predeploy();
    }

    protected void executePackaging() throws IOException {
        OutputStream out = new FileOutputStream(war);
        ZipOutputStream zout = new ZipOutputStream(out);
        try {
            String webInfDir = "WEB-INF/";
            String webInfLibDir = webInfDir + "lib/";
            zipTree("", new File(nxserver, "nuxeo.war"), false, zout);
            zipTree(webInfDir, new File(nxserver, "config"), false, zout);
            zipTree(webInfLibDir, new File(nxserver, "bundles"), false, zout);
            zipTree(webInfLibDir, new File(nxserver, "lib"), false, zout);
            // copy missing libs
            File lib = new File(nxserver.getParent(), "lib");
            for (String name : lib.list()) {
                for (String pat : MISSING_LIBS) {
                    String prefix = pat + '-';
                    if (name.startsWith(prefix) && name.endsWith(".jar")
                            && Character.isDigit(name.charAt(prefix.length()))) {
                        zipFile(new File(lib, name), webInfLibDir + name, zout,
                                null);
                        break;
                    }
                }
            }
        } finally {
            if (zout != null) {
                zout.finish();
                zout.close();
            }
        }
    }

    protected void zipDirectory(String entryName, ZipOutputStream zout)
            throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        zout.putNextEntry(zentry);
        zout.closeEntry();
    }

    protected void zipFile(File file, String entryName, ZipOutputStream zout,
            FileProcessor processor) throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        if (processor == null) {
            processor = CopyProcessor.INSTANCE;
            zentry.setTime(file.lastModified());
        }
        zout.putNextEntry(zentry);
        processor.process(file, zout);
        zout.closeEntry();
    }

    /** prefix ends with '/' */
    protected void zipTree(String prefix, File root, boolean includeRoot,
            ZipOutputStream zout) throws IOException {
        if (includeRoot) {
            prefix += root.getName() + '/';
            zipDirectory(prefix, zout);
        }
        for (String name : root.list()) {
            File file = new File(root, name);
            if (file.isDirectory()) {
                zipTree(prefix, file, true, zout);
            } else {
                if (name.endsWith("~") //
                        || name.endsWith("#") //
                        || name.endsWith(".bak") //
                        || name.equals("README.txt")) {
                    continue;
                }
                name = prefix + name;
                FileProcessor processor;
                if (name.equals("WEB-INF/web.xml")) {
                    processor = WebXmlProcessor.INSTANCE;
                } else {
                    processor = null;
                }
                zipFile(file, name, zout, processor);
            }
        }
    }

    protected interface FileProcessor {
        void process(File file, OutputStream out) throws IOException;
    }

    protected static class CopyProcessor implements FileProcessor {

        public static CopyProcessor INSTANCE = new CopyProcessor();

        @Override
        public void process(File file, OutputStream out) throws IOException {
            FileInputStream in = new FileInputStream(file);
            try {
                IOUtils.copy(in, out);
            } finally {
                in.close();
            }
        }
    }

    protected static class WebXmlProcessor implements FileProcessor {

        public static WebXmlProcessor INSTANCE = new WebXmlProcessor();

        private static final String LISTENER = "listener";

        private static final String LISTENER_CLASS = "listener-class";

        @Override
        public void process(File file, OutputStream zout) throws IOException {
            DocumentBuilder parser;
            try {
                parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw (IOException) new IOException().initCause(e);
            }
            InputStream in = new FileInputStream(file);
            try {
                Document doc = parser.parse(in);
                doc.setStrictErrorChecking(false);
                Node c = doc.getDocumentElement().getFirstChild();
                while (c != null) {
                    if (LISTENER.equals(c.getNodeName())) {
                        // insert initial listener
                        Element listener = doc.createElement(LISTENER);
                        c.insertBefore(listener, c);
                        listener.appendChild(doc.createElement(LISTENER_CLASS)).appendChild(
                                doc.createTextNode(NuxeoStarter.class.getName()));
                        break;
                    }
                    c = c.getNextSibling();
                }
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(new DOMSource(doc), new StreamResult(zout));
            } catch (SAXException e) {
                throw (IOException) new IOException().initCause(e);
            } catch (TransformerException e) {
                throw (IOException) new IOException().initCause(e);
            } finally {
                in.close();
            }
        }
    }

    public static void fail(String message) {
        fail(message, null);
    }

    public static void fail(String message, Throwable t) {
        log.error(message, t);
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2
                || args.length > 3
                || (args.length == 3 && !Arrays.asList(COMMAND_PREPROCESSING,
                        COMMAND_PACKAGING).contains(args[2]))) {
            fail(String.format(
                    "Usage: %s <nxserver_dir> <target_war> [command]\n"
                            + "    command may be empty or '%s' or '%s'",
                    PackWar.class.getSimpleName(), COMMAND_PREPROCESSING,
                    COMMAND_PACKAGING));
        }

        File nxserver = new File(args[0]).getAbsoluteFile();
        File war = new File(args[1]).getAbsoluteFile();
        String command = args.length == 3 ? args[2] : null;

        log.info("Packing nuxeo WAR at " + nxserver + " into " + war);
        try {
            new PackWar(nxserver, war).execute(command);
        } catch (Exception e) {
            fail("Pack failed", e);
        }
    }

}
