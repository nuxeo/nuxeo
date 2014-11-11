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
package org.nuxeo.ecm.shell.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class FileSystem {

    public static final String KEY = "fs";

    protected List<File> wdStack;

    public FileSystem() {
        wdStack = new ArrayList<File>();
        try {
            wdStack.add(new File(".").getCanonicalFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<File> getStack() {
        return wdStack;
    }

    public File pwd() {
        return wdStack.get(wdStack.size() - 1);
    }

    public void cd(File dir) {
        wdStack.clear();
        try {
            wdStack.add(dir.getCanonicalFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File pushd(File dir) {
        File lastWd = pwd();
        try {
            wdStack.add(dir.getCanonicalFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lastWd;
    }

    public File popd() {
        if (wdStack.size() > 1) {
            return wdStack.remove(wdStack.size() - 1);
        }
        return null;
    }

    public File resolveFile(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        } else if (path.startsWith("~/")) {
            return new File(System.getProperty("user.home"), path.substring(2));
        } else {
            return new File(pwd(), path);
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

    public static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = createBuffer(in.available());
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static String readContent(InputStream in) throws IOException {
        StringBuilder buf = new StringBuilder();
        byte[] bytes = new byte[1024 * 16];
        int r = -1;
        while ((r = in.read(bytes)) > -1) {
            buf.append(new String(bytes, 0, r));
        }
        return buf.toString();
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

    public static List<String> readAndMergeLines(InputStream in)
            throws IOException {
        List<String> lines = readLines(in);
        ArrayList<String> result = new ArrayList<String>();
        StringBuilder lastLine = null;
        for (String line : lines) {
            if (line.endsWith("\\")) {
                line = line.substring(0, line.length() - 1);
                if (lastLine != null) {
                    lastLine.append(line);
                } else {
                    lastLine = new StringBuilder(line);
                }
            } else {
                if (lastLine != null) {
                    result.add(lastLine.append(line).toString());
                    lastLine = null;
                } else {
                    result.add(line);
                }
            }
        }
        if (lastLine != null) {
            result.add(lastLine.toString());
        }
        return result;
    }

    private static final int BUFFER_SIZE = 1024 * 64; // 64K

    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 64K

    private static final int MIN_BUFFER_SIZE = 1024 * 8; // 64K

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

}
