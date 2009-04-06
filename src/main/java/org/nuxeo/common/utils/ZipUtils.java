/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 *
 * $$Id$$
 */

package org.nuxeo.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author bstefanescu
 */
public final class ZipUtils {

    // This is an utility class
    private ZipUtils() {
    }

    // _____________________________ ZIP ________________________________

    public static void _putDirectoryEntry(String entryName, ZipOutputStream out)
            throws IOException {
        ZipEntry zentry = new ZipEntry(entryName + '/');
        out.putNextEntry(zentry);
        out.closeEntry();
    }

    public static void _putFileEntry(File file, String entryName,
            ZipOutputStream out) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            _zip(entryName, in, out);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void _zip(String entryName, InputStream in,
            ZipOutputStream out) throws IOException {
        ZipEntry zentry = new ZipEntry(entryName);
        out.putNextEntry(zentry);
        // Transfer bytes from the input stream to the ZIP file
        FileUtils.copy(in, out);
        out.closeEntry();
    }

    public static void _zip(String entryName, File file, ZipOutputStream out)
            throws IOException {
        // System.out.println("Compressing "+entryName);
        if (file.isDirectory()) {
            entryName += '/';
            ZipEntry zentry = new ZipEntry(entryName);
            out.putNextEntry(zentry);
            out.closeEntry();
            File[] files = file.listFiles();
            for (int i = 0, len = files.length; i < len; i++) {
                _zip(entryName + files[i].getName(), files[i], out);
            }
        } else {
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                _zip(entryName, in, out);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    public static void _zip(File[] files, ZipOutputStream out, String prefix)
            throws IOException {
        if (prefix != null) {
            int len = prefix.length();
            if (len == 0) {
                prefix = null;
            } else if (prefix.charAt(len - 1) != '/') {
                prefix += '/';
            }
        }
        for (int i = 0, len = files.length; i < len; i++) {
            String name = prefix != null ? prefix + files[i].getName()
                    : files[i].getName();
            _zip(name, files[i], out);
        }
    }

    public static void zip(File file, OutputStream out, String prefix)
            throws IOException {
        if (prefix != null) {
            int len = prefix.length();
            if (len == 0) {
                prefix = null;
            } else if (prefix.charAt(len - 1) != '/') {
                prefix += '/';
            }
        }
        String name = prefix != null ? prefix + file.getName() : file.getName();
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(out);
            _zip(name, file, zout);
        } finally {
            if (zout != null) {
                zout.finish();
            }
        }
    }

    public static void zip(File[] files, OutputStream out, String prefix)
            throws IOException {
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(out);
            _zip(files, zout, prefix);
        } finally {
            if (zout != null) {
                zout.finish();
            }
        }
    }

    public static void zip(File file, File zip) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(zip));
            zip(file, out, null);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void zip(File[] files, File zip) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(zip));
            zip(files, out, null);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void zip(File file, File zip, String prefix)
            throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(zip));
            zip(file, out, prefix);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void zip(File[] files, File zip, String prefix)
            throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(zip));
            zip(files, out, prefix);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void zipFilesUsingPrefix(String prefix, File[] files,
            OutputStream out) throws IOException {
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(out);
            if (prefix != null && prefix.length() > 0) {
                int p = prefix.indexOf('/');
                while (p > -1) {
                    _putDirectoryEntry(prefix.substring(0, p), zout);
                    p = prefix.indexOf(p + 1, '/');
                }
                _putDirectoryEntry(prefix, zout);
                prefix += '/';
            } else {
                prefix = "";
            }
            // prefix = prefix + '/';
            for (File file : files) {
                _putFileEntry(file, prefix + file.getName(), zout);
            }
        } finally {
            if (zout != null) {
                zout.finish();
            }
        }
    }

    // _____________________________ UNZIP ________________________________

    public static void unzip(String prefix, InputStream zipStream, File dir)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(zipStream));
            unzip(prefix, in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(InputStream zipStream, File dir)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(zipStream));
            unzip(in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(String prefix, URL zip, File dir)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(zip.openStream()));
            unzip(prefix, in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(URL zip, File dir) throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(zip.openStream()));
            unzip(in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(String prefix, File zip, File dir)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(
                    new FileInputStream(zip)));
            unzip(prefix, in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(File zip, File dir) throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(
                    new FileInputStream(zip)));
            unzip(in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(String prefix, ZipInputStream in, File dir)
            throws IOException {
        dir.mkdirs();
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            if (!entry.getName().startsWith(prefix)) {
                entry = in.getNextEntry();
                continue;
            }
            File file = new File(dir, entry.getName().substring(prefix.length()));
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                FileUtils.copyToFile(in, file);
            }
            entry = in.getNextEntry();
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
                FileUtils.copyToFile(in, file);
            }
            entry = in.getNextEntry();
        }
    }

    public static void unzipIgnoreDirs(ZipInputStream in, File dir)
            throws IOException {
        dir.mkdirs();
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            String entryName = entry.getName();
            if (entry.isDirectory()) {
            } else {
                int p = entryName.lastIndexOf('/');
                if (p > -1) {
                    entryName = entryName.substring(p + 1);
                }
                File file = new File(dir, entryName);
                FileUtils.copyToFile(in, file);
            }
            entry = in.getNextEntry();
        }
    }

    public static void unzipIgnoreDirs(InputStream zipStream, File dir)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(zipStream));
            unzipIgnoreDirs(in, dir);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(File zip, File dir, PathFilter filter)
            throws IOException {
        ZipInputStream in = null;
        try {
            in = new ZipInputStream(new BufferedInputStream(
                    new FileInputStream(zip)));
            unzip(in, dir, filter);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void unzip(ZipInputStream in, File dir, PathFilter filter)
            throws IOException {
        if (filter == null) {
            unzip(in, dir);
            return;
        }
        ZipEntry entry = in.getNextEntry();
        while (entry != null) {
            String entryName = entry.getName();
            if (filter.accept(new Path(entryName))) {
                // System.out.println("Extracting "+entryName);
                File file = new File(dir, entryName);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    FileUtils.copyToFile(in, file);
                }
            }
            entry = in.getNextEntry();
        }
    }

    // ________________ Entries ________________
    /**
     * Unzip directly the entry. The returned InputStream has to be closed.
     *
     * @return the input stream of the desired entry - has to be closed by the
     *         caller, or null if not found
     * @param file
     *            the source file
     * @param entryName
     *            teh entry name that has to be extracted
     * @throws IOException
     * @author lgodard <lgodard@nuxeo.com>
     */
    public static InputStream getEntryContentAsStream(File file,
            String entryName) throws IOException {
        InputStream result = null;
        ZipFile zip = new ZipFile(file);
        ZipEntry entry = zip.getEntry(entryName);
        if (entry != null) {
            result = zip.getInputStream(entry);
        }
        return result;
    }

    /**
     * Unzip directly the entry.
     *
     * @return the String content of the entry with name entryName
     * @param file
     *            the source file
     * @param entryName
     *            teh entry name that has to be extracted
     * @throws IOException
     * @author lgodard <lgodard@nuxeo.com>
     */
    public static String getEntryContentAsString(File file,
            String entryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(file,
                entryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.read(resultStream);
    }

    /**
     * Unzips directly the entry.
     *
     * @return The byte array content of the entry with name entryName
     * @param file
     *            the source file
     * @param entryName
     *            teh entry name that has to be extracted
     * @throws IOException
     * @author lgodard <lgodard@nuxeo.com>
     */
    public static byte[] getEntryContentAsBytes(File file,
            String entryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(file,
                entryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.readBytes(resultStream);
    }

    /**
     * Lists the entries on the zip file.
     *
     * @param file
     *            The zip file
     * @return The list of entries
     * @throws IOException
     */
    public static List<String> getEntryNames(File file) throws IOException {
        List<String> result = new ArrayList<String>();
        ZipFile zip = new ZipFile(file);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                result.add(entry.getName());
            }
        } finally {
            zip.close();
        }
        return result;
    }

    /**
     * Checks if a zip file contains a specified entry name.
     *
     * @param file
     *            the zip file
     * @param entryName
     *            The content to be checked
     * @return True if the file contains entryName. False otherwise
     * @throws IOException
     */
    public static boolean hasEntry(File file, String entryName)
            throws IOException {
        List<String> elements = getEntryNames(file);
        return elements.contains(entryName);
    }

    public static InputStream getEntryContentAsStream(InputStream stream,
            String entryName) throws IOException {
        ZipInputStream zip = new ZipInputStream(stream);
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {
            if (entry.getName().equals(entryName)) {
                return zip;
            }
            entry = zip.getNextEntry();
        }
        return null;
    }

    public static String getEntryContentAsString(InputStream stream,
            String searchedEntryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(stream,
                searchedEntryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.read(resultStream);
    }

    public static byte[] getEntryContentAsBytes(InputStream stream,
            String searchedEntryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(stream,
                searchedEntryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.readBytes(resultStream);
    }

    public static List<String> getEntryNames(InputStream stream)
            throws IOException {

        List<String> result = new ArrayList<String>();
        ZipInputStream zip = new ZipInputStream(stream);
        try {
            while (zip.available() == 1) {
                ZipEntry entry = zip.getNextEntry();
                if (entry != null) {
                    result.add(entry.getName());
                }
            }
        } finally {
            zip.close();
        }
        return result;
    }

    public static boolean hasEntry(InputStream stream, String entryName)
            throws IOException {
        List<String> elements = getEntryNames(stream);
        return elements.contains(entryName);
    }

    public static InputStream getEntryContentAsStream(URL url,
            String entryName) throws IOException {
        return getEntryContentAsStream(url.openStream(), entryName);
    }

    public static String getEntryContentAsString(URL url,
            String entryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(url,
                entryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.read(resultStream);
    }

    public static byte[] getEntryContentAsBytes(URL url,
            String entryName) throws IOException {
        InputStream resultStream = getEntryContentAsStream(url,
                entryName);
        // resultStream has been closed by FileUtils call
        return FileUtils.readBytes(resultStream);
    }

    public static List<String> getEntryNames(URL url) throws IOException {
        return getEntryNames(url.openStream());
    }

    public static boolean hasEntry(URL url, String entryName)
            throws IOException {
        return hasEntry(url.openStream(), entryName);
    }

}
