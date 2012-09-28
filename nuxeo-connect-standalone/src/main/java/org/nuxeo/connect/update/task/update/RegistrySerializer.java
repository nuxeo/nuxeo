/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RegistrySerializer extends XmlWriter {

    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * @since 5.7
     */
    public RegistrySerializer() {
        super("  ");
    }

    /**
     * Serializes the given registry into the given file.
     *
     * @param registry
     * @param file
     * @throws IOException
     */
    public static void store(Map<String, Entry> registry, File file)
            throws IOException {
        RegistrySerializer serializer = new RegistrySerializer();
        serializer.write(registry);
        serializer.write(file);
    }

    /**
     * De-serializes the given file into a Nuxeo packages registry
     *
     * @param file
     * @return The Nuxeo packages registry described by the given file
     * @throws PackageException
     * @throws FileNotFoundException
     */
    public static Map<String, Entry> load(File file) throws PackageException,
            FileNotFoundException {
        RegistrySerializer serializer = new RegistrySerializer();
        return serializer.read(file);
    }

    /**
     * @param file
     * @return
     * @throws PackageException
     * @throws FileNotFoundException
     */
    protected Map<String, Entry> read(File file) throws PackageException,
            FileNotFoundException {
        FileInputStream in = new FileInputStream(file);
        try {
            HashMap<String, Entry> registry = new HashMap<String, Entry>();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            read(document.getDocumentElement(), registry);
            return registry;
        } catch (PackageException e) {
            throw e;
        } catch (Throwable e) {
            throw new PackageException("Failed to load file update registry", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    protected void read(Element element, Map<String, Entry> registry)
            throws PackageException {
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && "entry".equals(node.getNodeName())) {
                Entry entry = readEntryElement((Element) node);
                registry.put(entry.getKey(), entry);
            }
            node = node.getNextSibling();
        }
    }

    protected Entry readEntryElement(Element element) throws PackageException {
        Entry entry = new Entry(readKeyAttr(element));
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getNodeName();
                if ("version".equals(name)) {
                    Version v = readVersionElement((Element) node);
                    entry.addVersion(v);
                } else if ("base-version".equals(name)) {
                    Version v = readVersionElement((Element) node);
                    entry.setBaseVersion(v);
                }
            }
            node = node.getNextSibling();
        }
        return entry;
    }

    protected String readKeyAttr(Element element) throws PackageException {
        String key = element.getAttribute("key");
        if (key.length() == 0) {
            throw new PackageException(
                    "Invalid entry. No 'key' attribute found!");
        }
        return key;
    }

    protected String readNameAttr(Element element) throws PackageException {
        String version = element.getAttribute("name");
        if (version.length() == 0) {
            throw new PackageException(
                    "Invalid version entry. No 'name' attribute found!");
        }
        return version;
    }

    protected String readPathAttr(Element element) throws PackageException {
        String path = element.getAttribute("path");
        if (path.length() == 0) {
            throw new PackageException(
                    "Invalid version entry. No 'path' attribute found!");
        }
        return path;
    }

    protected Version readVersionElement(Element element)
            throws PackageException {
        Version v = new Version(readNameAttr(element));
        v.setPath(readPathAttr(element));
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && "package".equals(node.getNodeName())) {
                UpdateOptions opt = new UpdateOptions();
                opt.pkgId = ((Element) node).getTextContent().trim();
                opt.upgradeOnly = Boolean.parseBoolean(((Element) node).getAttribute("upgradeOnly"));
                v.addPackage(opt);
            }
            node = node.getNextSibling();
        }
        return v;
    }

    protected void write(Map<String, Entry> registry) {
        writeXmlDecl();
        start("registry");
        startContent();
        for (Entry entry : registry.values()) {
            writeEntry(entry);
        }
        end("registry");
    }

    protected void writeEntry(Entry entry) {
        start("entry");
        attr("key", entry.getKey());
        startContent();
        if (entry.hasBaseVersion()) {
            writeBaseVersion(entry.getBaseVersion());
        }
        for (Version v : entry) {
            writeVersion(v);
        }
        end("entry");
    }

    protected void writeBaseVersion(Version version) {
        start("base-version");
        attr("name", version.getVersion());
        attr("path", version.getPath());
        end();
    }

    protected void writeVersion(Version version) {
        start("version");
        attr("name", version.getVersion());
        attr("path", version.getPath());
        startContent();
        Map<String, UpdateOptions> packages = version.getPackages();
        for (UpdateOptions opt : packages.values()) {
            start("package");
            if (opt.upgradeOnly) {
                attr("upgradeOnly", "true");
            }
            // Missing methods to properly append the following without indent
            text(">" + opt.pkgId + "</package>\n");
            // startContent(false);
            // text(opt.pkgId);
            // end("package", false);
        }
        end("version");
    }

    /**
     * @param file Output file
     * @throws IOException
     * @since 5.7
     */
    protected void write(File file) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        try {
            writer.write(sb.toString());
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

}
