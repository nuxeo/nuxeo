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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RegistrySerializer {

    private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    public static void store(Map<String, Entry> registry, File file)
            throws IOException {
        FileOutputStream in = new FileOutputStream(file);
        try {
            store(registry, in);
        } finally {
            in.close();
        }
    }

    public static void store(Map<String, Entry> registry, OutputStream out)
            throws IOException {
        Writer writer = new OutputStreamWriter(out);
        store(registry, writer);
        writer.flush();
    }

    public static void store(Map<String, Entry> registry, Writer out)
            throws IOException {
        XmlWriter writer = new XmlWriter("  ");
        write(registry, writer);
        out.write(writer.toString());
    }

    public static Map<String, Entry> load(File file) throws PackageException,
            IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public static Map<String, Entry> load(InputStream in)
            throws PackageException {
        HashMap<String, Entry> registry = new HashMap<String, Entry>();
        try {
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            read(document.getDocumentElement(), registry);
            return registry;
        } catch (PackageException e) {
            throw e;
        } catch (Throwable e) {
            throw new PackageException("Failed to load file update registry", e);
        }
    }

    public static void read(Element element, Map<String, Entry> registry)
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

    public static Entry readEntryElement(Element element)
            throws PackageException {

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

    public static String readKeyAttr(Element element) throws PackageException {
        String key = element.getAttribute("key");
        if (key.length() == 0) {
            throw new PackageException(
                    "Invalid entry. No 'key' attribute found!");
        }
        return key;
    }

    public static String readNameAttr(Element element) throws PackageException {
        String version = element.getAttribute("name");
        if (version.length() == 0) {
            throw new PackageException(
                    "Invalid version entry. No 'name' attribute found!");
        }
        return version;
    }

    public static String readPathAttr(Element element) throws PackageException {
        String path = element.getAttribute("path");
        if (path.length() == 0) {
            throw new PackageException(
                    "Invalid version entry. No 'path' attribute found!");
        }
        return path;
    }

    public static Version readVersionElement(Element element)
            throws PackageException {
        Version v = new Version(readNameAttr(element));
        v.setPath(readPathAttr(element));

        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && "package".equals(node.getNodeName())) {
                v.addPackage(((Element) node).getTextContent().trim());
            }
            node = node.getNextSibling();
        }

        return v;
    }

    public static void write(Map<String, Entry> registry, XmlWriter writer) {
        writer.writeXmlDecl();
        writer.start("registry");
        writer.startContent();
        for (Entry entry : registry.values()) {
            writeEntry(entry, writer);
        }
        writer.end("registry");
    }

    public static void writeEntry(Entry entry, XmlWriter writer) {
        writer.start("entry");
        writer.attr("key", entry.getKey());
        writer.startContent();
        if (entry.hasBaseVersion()) {
            writeBaseVersion(entry.getBaseVersion(), writer);
        }
        for (Version v : entry) {
            writeVersion(v, writer);
        }
        writer.end("entry");
    }

    public static void writeBaseVersion(Version version, XmlWriter writer) {
        writer.start("base-version");
        writer.attr("name", version.getVersion());
        writer.attr("path", version.getPath());
        writer.end();
        // writer.startContent();
        // for (String pkg : version.getPackages()) {
        // writer.element("package", pkg);
        // }
        // writer.end("base-version");
    }

    public static void writeVersion(Version version, XmlWriter writer) {
        writer.start("version");
        writer.attr("name", version.getVersion());
        writer.attr("path", version.getPath());
        writer.startContent();
        for (String pkg : version.getPackages()) {
            writer.element("package", pkg);
        }
        writer.end("version");
    }

}
