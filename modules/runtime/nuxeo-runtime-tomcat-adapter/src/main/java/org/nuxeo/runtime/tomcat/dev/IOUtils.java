/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class IOUtils {

    public static void deleteTree(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteTree(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    public static void copyTree(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdir();
            }
            for (File child : source.listFiles()) {
                copyTree(child, new File(target, child.getName()));
            }
        } else {
            try (FileInputStream in = new FileInputStream(source); //
                    FileOutputStream out = new FileOutputStream(target)) {
                copyContent(in, out);
            }
        }
    }

    public static void copyContent(InputStream is, OutputStream out) throws IOException {
        int data;
        while (is.available() > 0 && (data = is.read()) != -1) {
            out.write(data);
        }
    }

    public static void appendResourceBundleFragments(String name, List<File> files, File target) throws IOException {
        File l10n = new File(target, name);
        File backup = new File(target, name + "~bak");
        if (!backup.exists()) {
            backup.createNewFile();
            try (FileInputStream in = new FileInputStream(l10n); //
                    FileOutputStream out = new FileOutputStream(backup)) {
                copyContent(in, out);
            }
        }
        try (FileInputStream in = new FileInputStream(backup); //
                FileOutputStream out = new FileOutputStream(l10n)) {
            copyContent(in, out);
        }
        for (File file : files) {
            try (InputStream in = new FileInputStream(file); //
                    OutputStream out = new FileOutputStream(l10n, true)) { // append
                copyContent(in, out);
            }
        }
    }

}
