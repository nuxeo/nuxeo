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
 *     bstefanescu
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Archetype {

    private static final int BUFFER_SIZE = 1024 * 64; // 64K

    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K

    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K

    static boolean batchMode = false;

    static String outDir = "${artifactId}";

    static File archive;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Syntax Error: you must specify a project template name");
        }
        int k = 0;
        String tpl = args[k];
        if ("-b".equals(tpl)) {
            batchMode = true;
            if (args.length < 2) {
                System.err.println("Syntax Error: you must specify a project template name");
            }
            tpl = args[++k];
        }
        k++;
        archive = new File(tpl);
        if (args.length > k) {
            outDir = args[k];
        }
        ZipFile zip = new ZipFile(archive);
        ZipEntry entry = zip.getEntry("archetype.xml");
        if (entry == null) {
            System.err.println("Invalid archetype zip.");
            System.exit(1);
        }
        // load archetype definition
        InputStream in = new BufferedInputStream(zip.getInputStream(entry));
        Document doc = load(in);
        zip.close();
        // process it
        processArchetype(doc, System.getProperties());
    }

    private static void expandVars(File file, Map<?, ?> vars) throws IOException {
        String content = readFile(file);
        content = expandVars(content, vars);
        writeFile(file, content);
    }

    public static void unzip(File zip, File dir) throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
            unzip(in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(ZipInputStream in, File dir) throws IOException {
        dir.mkdirs();
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            // System.out.println("Extracting "+entry.getName());
            File file = new File(dir, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                copyToFile(in, file);
            }
            entry = in.getNextEntry();
        }
    }

    public static String getShortName(String name) {
        int p = name.lastIndexOf('.');
        if (p > -1) {
            return name.substring(0, p);
        }
        return name;
    }

    public static String expandVars(String expression, Map<?, ?> properties) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$':
                dollar = true;
                break;
            case '{':
                if (dollar) {
                    dollar = false;
                    var = true;
                } else {
                    result.append(c);
                }
                break;
            case '}':
                if (var) {
                    var = false;
                    String varName = varBuf.toString();
                    varBuf.setLength(0);
                    // get the variable value
                    Object varValue = properties.get(varName);
                    if (varValue != null) {
                        result.append(varValue.toString());
                    } else { // let the variable as is
                        result.append("${").append(varName).append('}');
                    }
                } else {
                    result.append(c);
                }
                break;
            default:
                if (var) {
                    varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

    public static String readFile(File file) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return read(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = createBuffer(in.available());
        try {
            int read;
            while ((read = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, read));
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

    public static void copyToFile(InputStream in, File file) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            byte[] buffer = createBuffer(in.available());
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static byte[] createBuffer(int preferredSize) {
        if (preferredSize < 1) {
            preferredSize = BUFFER_SIZE;
        }
        if (preferredSize > MAX_BUFFER_SIZE) {
            preferredSize = MAX_BUFFER_SIZE;
        } else if (preferredSize < MIN_BUFFER_SIZE) {
            preferredSize = MIN_BUFFER_SIZE;
        }
        return new byte[preferredSize];
    }

    public static Document load(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    public static Document load(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(in);
    }

    public static void processVars(Element root, Map<Object, Object> vars) throws IOException {
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("var".equals(el.getNodeName())) {
                    String key = el.getAttribute("name");
                    String label = el.getAttribute("label");
                    if (label == null) {
                        label = key;
                    }
                    String val = (String) vars.get(key);
                    String def = el.getAttribute("default");
                    if (def != null) {
                        def = expandVars(def, vars);
                    } else {
                        def = val;
                    }
                    if (!batchMode && val == null && "true".equals(el.getAttribute("prompt"))) {
                        val = readVar(label, def);
                    }
                    if (val == null) {
                        val = def;
                    }
                    vars.put(key, val);
                }
            }
            node = node.getNextSibling();
        }
    }

    public static void processResources(Element root, File dir, Map<Object, Object> vars) throws IOException {
        Node node = root.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("directory".equals(el.getNodeName())) {
                    String srcName = el.getAttribute("src");
                    if (srcName == null) {
                        throw new IllegalArgumentException("directory has no src attribute");
                    }
                    String targetName = el.getAttribute("target");
                    if (targetName == null) {
                        throw new IllegalArgumentException("directory has no target attribute");
                    }
                    srcName = expandVars(srcName, vars);
                    targetName = expandVars(targetName, vars);
                    File src = new File(dir, srcName);
                    File dst = new File(dir, targetName);
                    System.out.println("Renaming " + src + " to " + dst);
                    src.renameTo(dst);
                } else if ("package".equals(el.getNodeName())) {
                    String srcName = el.getAttribute("src");
                    if (srcName == null) {
                        throw new IllegalArgumentException("package has no src attribute");
                    }
                    String targetName = el.getAttribute("target");
                    if (targetName == null) {
                        throw new IllegalArgumentException("package has no target attribute");
                    }
                    srcName = expandVars(srcName, vars);
                    targetName = expandVars(targetName, vars);
                    targetName = targetName.replaceAll("\\.", "/");
                    File src = new File(dir, srcName);
                    File dst = new File(dir, targetName);
                    System.out.println("Renaming " + src + " to " + dst);
                    dst.getParentFile().mkdirs();
                    src.renameTo(dst);
                } else if ("template".equals(el.getNodeName())) {
                    String srcName = el.getAttribute("src");
                    if (srcName == null) {
                        throw new IllegalArgumentException("rename has no src attribute");
                    }
                    File src = new File(dir, srcName);
                    System.out.println("Processing " + src);
                    expandVars(src, vars);
                }
            }
            node = node.getNextSibling();
        }
    }

    public static void processArchetype(Document doc, Map<Object, Object> vars) throws IOException {
        Element root = doc.getDocumentElement();
        Node node = root.getFirstChild();
        Element elVars = null;
        Element elRes = null;
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("vars".equals(el.getNodeName())) {
                    elVars = el;
                } else if ("resources".equals(el.getNodeName())) {
                    elRes = el;
                }
            }
            node = node.getNextSibling();
        }
        if (elVars != null) {
            processVars(elVars, vars);
        }
        // System.out.println("vars: "+System.getProperty("artifactId")+" - "+System.getProperty("groupId")+" = "+System.getProperty("moduleId"));
        outDir = expandVars(outDir, vars);
        File out = new File(outDir);
        if (out.exists()) {
            System.out.println("Target directory already exists: " + out);
            System.out.println("Please specify as target a directory to be created. Exiting.");
            System.exit(1);
        }
        unzip(archive, out);
        new File(out, "archetype.xml").delete();
        if (elRes != null) {
            processResources(elRes, out, vars);
        }
    }

    public static String readVar(String key, String value) throws IOException {
        System.out.print(key + (value == null ? ": " : " [" + value + "]: "));
        StringBuilder buf = new StringBuilder();
        int c = System.in.read();
        LOOP: while (c != -1) {
            if (c == '\n' || c == '\r') {
                if (buf.length() == 0) {
                    if (value == null) {
                        System.out.println(key + ": ");
                        break LOOP;
                    } else {
                        return value;
                    }
                }
                return buf.toString();
            }
            buf.append((char) c);
            c = System.in.read();
        }
        return value;
    }

    public static File unzipArchetype(File zipFile) throws IOException {
        File file = Framework.createTempFile("nuxeo_archetype_" + zipFile.getName(), ".tmp");
        Framework.trackFile(file, file);
        file.delete();
        file.mkdirs();
        unzip(zipFile, file);
        return file;
    }

    public static void deleteTree(File dir) {
        emptyDirectory(dir);
        dir.delete();
    }

    public static void emptyDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        int len = files.length;
        for (int i = 0; i < len; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                deleteTree(file);
            } else {
                file.delete();
            }
        }
    }

    public static void writeFile(File file, byte[] buf) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(buf);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void writeFile(File file, String buf) throws IOException {
        writeFile(file, buf.getBytes());
    }

    public static void launch(String[] args) {
        // launch an app.
    }

}
