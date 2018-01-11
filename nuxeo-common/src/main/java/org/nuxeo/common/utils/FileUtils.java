/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu <bs@nuxeo.com>
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class FileUtils {

    private static final Log log = LogFactory.getLog(FileUtils.class);

    // This is an utility class
    private FileUtils() {
    }

    /**
     * Copies source to destination. If source and destination are the same, does nothing. Both single files and
     * directories are handled.
     *
     * @param src the source file or directory
     * @param dst the destination file or directory
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

    /**
     * @deprecated since 10.1 - use {@link org.apache.commons.io.FileUtils#copyFile(File, File)} or
     *             {@link org.apache.commons.io.FileUtils#copyFileToDirectory(File, File)} instead.
     */
    @Deprecated
    public static void copyFile(File src, File dst) throws IOException {
        if (dst.isDirectory()) {
            dst = new File(dst, src.getName());
        }
        org.apache.commons.io.FileUtils.copyFile(src, dst, false);
    }

    /**
     * Copies recursively source to destination.
     * <p>
     * The source file is assumed to be a directory.
     *
     * @param src the source directory
     * @param dst the destination directory
     * @deprecated since 10.1 - waiting ReloadComponent to be cleaned
     */
    @Deprecated
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

    /**
     * @deprecated since 10.1 - seems unused
     */
    @Deprecated
    public static void copyTree(File src, File dst, PathFilter filter) throws IOException {
        copyTree(src, dst, new Path("/"), filter);
    }

    public static void copyTree(File src, File dst, Path prefix, PathFilter filter) throws IOException {
        if (!prefix.isAbsolute()) {
            prefix = prefix.makeAbsolute();
        }
        int rootIndex = src.getPath().length() + 1;
        for (File file : src.listFiles()) {
            copyTree(rootIndex, file, new File(dst, file.getName()), prefix, filter);
        }
    }

    protected static void copyTree(int rootIndex, File src, File dst, Path prefix, PathFilter filter)
            throws IOException {
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
                    path = path.substring(0, prefixIndex) + path.substring(prefixIndex + pff.length());
                    dst = new File(path.toString());
                }
            }
            dst.getParentFile().mkdirs();
            copyFile(src, dst);
        } else if (src.isDirectory()) {
            File[] files = src.listFiles();
            for (File file : files) {
                copyTree(rootIndex, file, new File(dst, file.getName()), prefix, filter);
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
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
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
        List<File> result = new ArrayList<>();
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
                collectFiles(file, result);
            }
        }
    }

    /**
     * Create a file handler (this doesn't create a real file) given a file URI. This method can be used to create files
     * from invalid URL strings (e.g. containing spaces ..)
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

    /**
     * Compares two files content as String even if their EOL are different
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
            return expected.replace("\r\n", "\n").equals(source.replace("\r\n", "\n"));
        } else {
            return expected.equals(source);
        }
    }

    /**
     * Returns a safe filename, replacing unsafe characters (: \ / * ..) with "_". For instance, it turns
     * "tmp/../2349:876398/foo.png" into "tmp___2349_876398_foo.png"
     *
     * @param filename the filename
     * @return the safe filename with underscores instead of unsafe characters
     * @since 9.1
     */
    public static String getSafeFilename(String filename) {
        return filename.replaceAll("(\\\\)|(\\/)|(\\:)|(\\*)|(\\.\\.)", "_");
    }

}
