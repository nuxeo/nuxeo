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
 * $Id$
 */

package org.nuxeo.common.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class FileUtils {

    private static final int BUFFER_SIZE = 1024 * 64; // 64K
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K
    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K

    private static final Log log = LogFactory.getLog(FileUtils.class);

    // This is an utility class
    private FileUtils() {
    }

    public static void safeClose(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            // do nothing
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

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = createBuffer(in.available());
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
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

    public static byte[] readBytes(URL url) throws IOException {
        return readBytes(url.openStream());
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        byte[] buffer = createBuffer(in.available());
        int w = 0;
        try {
            int read = 0;
            int len;
            do {
                w += read;
                len = buffer.length - w;
                if (len <= 0) { // resize buffer
                    byte[] b = new byte[buffer.length + BUFFER_SIZE];
                    System.arraycopy(buffer, 0, b, 0, w);
                    buffer = b;
                    len = buffer.length - w;
                }
            } while ((read = in.read(buffer, w, len)) != -1);
        } finally {
            in.close();
        }
        if (buffer.length > w) { // compact buffer
            byte[] b = new byte[w];
            System.arraycopy(buffer, 0, b, 0, w);
            buffer = b;
        }
        return buffer;
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

    public static List<String> readLines(File file) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            InputStream in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return lines;
    }

    public static void writeLines(File file, List<String> lines) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(file));
            for (String line : lines) {
                out.println(line);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static byte[] readBytes(File file) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return readBytes(in);
        } finally {
            if (in != null) {
                in.close();
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

    public static void download(URL url, File file) throws IOException {
        InputStream in = url.openStream();
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            if (in != null) {
                in.close();
            }
            out.close();
        }
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

    public static void append(File src, File dst) throws IOException {
        append(src, dst, false);
    }

    public static void append(File src, File dst, boolean appendNewLine) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(src);
            append(in, dst, appendNewLine);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static void append(InputStream in, File file) throws IOException {
        append(in, file, false);
    }

    public static void append(InputStream in, File file, boolean appendNewLine) throws IOException {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file, true));
            if (appendNewLine) {
                out.write(System.getProperty("line.separator").getBytes());
            }
            byte[] buffer = new byte[BUFFER_SIZE];
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

    /**
     * Copies source to destination. If source and destination are the same,
     * does nothing. Both single files and directories are handled.
     *
     * @param src the source file or directory
     * @param dst the destination file or directory
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        if (src.equals(dst)) {
            return;
        }
        if (src.isFile()) {
            copyFile(src, dst);
        } else {
            copyTree(src, dst);
        }
    }

    public static void copy(File[] src, File dst) throws IOException {
        for (File file : src) {
            copy(file, dst);
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (dst.isDirectory()) {
            dst = new File(dst, src.getName());
        }
        FileInputStream in = null;
        FileOutputStream out = new FileOutputStream(dst);
        try {
            in = new FileInputStream(src);
            copy(in, out);
        } finally {
            if (in != null) {
                in.close();
            }
            out.close();
        }
    }

    /**
     * Copies recursively source to destination.
     * <p>
     * The source file is assumed to be a directory.
     *
     * @param src the source directory
     * @param dst the destination directory
     * @throws IOException
     */
    public static void copyTree(File src, File dst) throws IOException {
        if (src.isFile()) {
            copyFile(src, dst);
        } else if (src.isDirectory()) {
            if (dst.exists()) {
                dst = new File(dst, src.getName());
                dst.mkdir();
            } else { // allows renaming dest dir
                dst.mkdirs();
            }
            File[] files = src.listFiles();
            for (File file : files) {
                copyTree(file, dst);
            }
        }
    }

    public static void copyTree(File src, File dst, PathFilter filter) throws IOException {
        if (!filter.accept(new Path(src.getAbsolutePath()))) {
            return;
        }
        if (src.isFile()) {
            copyFile(src, dst);
        } else if (src.isDirectory()) {
            if (dst.exists()) {
                dst = new File(dst, src.getName());
                dst.mkdir();
            } else { // allow renaming dest dir
                dst.mkdirs();
            }
            File[] files = src.listFiles();
            for (File file : files) {
                copyTree(file, dst, filter);
            }
        }
    }

    /**
     * Decodes an URL path so that is can be processed as a filename later.
     *
     * @param url the Url to be processed.
     * @return the decoded path.
     */
    public static String getFilePathFromUrl(URL url) {
        String path = "";
        if (url.getProtocol().equals("file")) {
            try {
                path = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error(e);
            }
        }
        return path;
    }

    public static File getFileFromURL(URL url) {
        File file;
        String filename = getFilePathFromUrl(url);
        if (filename.equals("")) {
            file = null;
        } else {
            file = new File(filename);
        }
        return file;
    }

    public static String getParentPath(String path) {
        int p = path.lastIndexOf('/');
        if (p == -1) {
            return null;
        }
        return path.substring(0, p);
    }

    public static String getFileName(String path) {
        int p = path.lastIndexOf('/');
        if (p == -1) {
            return path;
        }
        return path.substring(p + 1);
    }

    public static String getFileExtension(String path) {
        int p = path.lastIndexOf('.');
        if (p == -1) {
            return null;
        }
        return path.substring(p + 1);
    }

    public static String getFileNameNoExt(String path) {
        String name = getFileName(path);
        int p = name.lastIndexOf('.');
        if (p == -1) {
            return name;
        }
        return name.substring(0, p);
    }

    /**
     * Retrieves the total path of a resource from the Thread Context.
     *
     * @param resource the resource name to be retrieved.
     * @return the decoded path.
     */
    public static String getResourcePathFromContext(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                resource);
        return getFilePathFromUrl(url);
    }

    public static File getResourceFileFromContext(String resource) {
        File file;
        String filename = getResourcePathFromContext(resource);
        if (filename.equals("")) {
            file = null;
        } else {
            file = new File(filename);
        }
        return file;
    }

    public static File[] findFiles(File root, String pattern, boolean recurse) {
        List<File> result = new ArrayList<File>();
        if (pattern == null) {
            if (recurse) {
                collectFiles(root, result);
            } else {
                return root.listFiles();
            }
        } else {
            FileNamePattern pat = new FileNamePattern(pattern);
            if (recurse) {
                collectFiles(root, pat, result);
            } else {
                File[] files = root.listFiles();
                for (File file : files) {
                    if (pat.match(file.getName())) {
                        result.add(file);
                    }
                }
            }
        }
        return result.toArray(new File[result.size()]);
    }

    public static void collectFiles(File root, FileNamePattern pattern, List<File> result) {
        File[] files = root.listFiles();
        for (File file : files) {
            if (pattern.match(file.getName())) {
                result.add(file);
                if (file.isDirectory()) {
                    collectFiles(file, pattern, result);
                }
            }
        }
    }

    public static void collectFiles(File root, List<File> result) {
        File[] files = root.listFiles();
        for (File file : files) {
            result.add(file);
            if (file.isDirectory()) {
                collectFiles(root, result);
            }
        }
    }

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public static void close(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }


    /**
     * Create a file handler (this doesn't create a real file) given a file URI.
     * This method can be used to create files from invalid URL strings (e.g. containing spaces ..)
     * @return a file object
     */
    public static File urlToFile(String url) throws MalformedURLException {
        return urlToFile(new URL(url));
    }

    public static File urlToFile(URL url) {
        try {
          return new File(url.toURI());
        } catch(URISyntaxException e) {
          return new File(url.getPath());
        }
    }

    public static List<String> readLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return lines;
    }

}
