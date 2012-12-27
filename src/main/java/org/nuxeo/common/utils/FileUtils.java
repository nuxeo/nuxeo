/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
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

import org.apache.commons.io.IOUtils;
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

    public static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = createBuffer(in.available());
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Read the byte stream as a string assuming a UTF-8 encoding.
     *
     * @deprecated use org.apache.commons.io.IOUtils.toString(in, "UTF-8")
     *             explicitly instead (or any other encoding when provided by
     *             the source of the byte stream).
     */
    @Deprecated
    public static String read(InputStream in) throws IOException {
        // UTF-8 should is configured as the default "file.encoding" in a system
        // property configured in the nuxeo.conf file.
        // However this option might not be passed when running the Nuxeo as a
        // library or using the maven test runner. Therefore we hardcode the
        // default charset to "UTF-8" to ensure consistency.
        return IOUtils.toString(in, "UTF-8");
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

    public static void writeLines(File file, List<String> lines)
            throws IOException {
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
        writeFile(file, buf, false);
    }

    /**
     * @param file
     * @param buf
     * @param append
     * @throws IOException
     * @since 5.5
     */
    public static void writeFile(File file, byte[] buf, boolean append)
            throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, append);
            fos.write(buf);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void writeFile(File file, String buf) throws IOException {
        writeFile(file, buf.getBytes(), false);
    }

    /**
     * @param dst
     * @param content
     * @param append
     * @throws IOException
     * @since 5.5
     */
    public static void writeFile(File file, String buf, boolean append)
            throws IOException {
        writeFile(file, buf.getBytes(), append);
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

    /**
     * @deprecated Since 5.6. Use
     *             {@link org.apache.commons.io.FileUtils#deleteDirectory(File)}
     *             or
     *             {@link org.apache.commons.io.FileUtils#deleteQuietly(File)}
     *             instead.
     */
    @Deprecated
    public static void deleteTree(File dir) {
        emptyDirectory(dir);
        dir.delete();
    }

    /**
     * @deprecated Since 5.6. Use
     *             {@link org.apache.commons.io.FileUtils#deleteDirectory(File)}
     *             or
     *             {@link org.apache.commons.io.FileUtils#deleteQuietly(File)}
     *             instead. Warning: suggested methods will delete the root
     *             directory whereas current method doesn't.
     */
    @Deprecated
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

    public static void append(File src, File dst, boolean appendNewLine)
            throws IOException {
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

    public static void append(InputStream in, File file, boolean appendNewLine)
            throws IOException {
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
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dst);
            in = new FileInputStream(src);
            copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
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

    public static void copyTree(File src, File dst, PathFilter filter)
            throws IOException {
        copyTree(src, dst, new Path("/"), filter);
    }

    public static void copyTree(File src, File dst, Path prefix,
            PathFilter filter) throws IOException {
        if (!prefix.isAbsolute()) {
            prefix = prefix.makeAbsolute();
        }
        int rootIndex = src.getPath().length() + 1;
        for (File file : src.listFiles()) {
            copyTree(rootIndex, file, new File(dst, file.getName()), prefix,
                    filter);
        }
    }

    protected static void copyTree(int rootIndex, File src, File dst,
            Path prefix, PathFilter filter) throws IOException {
        if (src.isFile()) {
            String relPath = src.getPath().substring(rootIndex);
            if (!filter.accept(new Path(relPath))) {
                return;
            }
            if (!prefix.isRoot()) { // remove prefix from path
                String path = dst.getPath();
                String pff = prefix.toString();
                int prefixIndex = path.lastIndexOf(pff);
                if (prefixIndex > 0) {
                    path = path.substring(0, prefixIndex)
                            + path.substring(prefixIndex + pff.length());
                    dst = new File(path.toString());
                }
            }
            dst.getParentFile().mkdirs();
            copyFile(src, dst);
        } else if (src.isDirectory()) {
            File[] files = src.listFiles();
            for (File file : files) {
                copyTree(rootIndex, file, new File(dst, file.getName()),
                        prefix, filter);
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
        int p = path.lastIndexOf(File.separator);
        if (p == -1) {
            return null;
        }
        return path.substring(0, p);
    }

    public static String getFileName(String path) {
        int p = path.lastIndexOf(File.separator);
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

    public static void collectFiles(File root, FileNamePattern pattern,
            List<File> result) {
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
                collectFiles(file, result);
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
     * This method can be used to create files from invalid URL strings (e.g.
     * containing spaces ..)
     *
     * @return a file object
     */
    public static File urlToFile(String url) throws MalformedURLException {
        return urlToFile(new URL(url));
    }

    public static File urlToFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
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

    /**
     * Compares two files content as String even if their EOL are differents
     *
     * @param expected a file content with Windows or Unix like EOL
     * @param source another file content with Windows or Unix like EOL
     * @return the result of equals after replacing their EOL
     */
    public static boolean areFilesContentEquals(String expected, String source) {
        if (expected == source) {
            return true;
        }

        if (expected == null || source == null) {
            return false;
        }

        if (expected.length() != source.length()) {
            // Prevent from comparing files with Windows EOL
            return expected.replace("\r\n", "\n").equals(
                    source.replace("\r\n", "\n"));
        } else {
            return expected.equals(source);
        }
    }

}
