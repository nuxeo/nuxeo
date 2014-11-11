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
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No classpath specified");
            System.exit(10);
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            System.err.println("Not a valid class loader: "+cl);
            System.exit(10);
        }
        // build the class path now
        List<File> cp = buildClassPath(args[0]);
        // make new arguments by removing the first one to pass further
        String[] tmp = new String[args.length - 1];
        System.arraycopy(args, 1, tmp, 0, tmp.length);
        args = tmp;
        StandaloneApplication.main(cp, args);
    }

    public static List<File> buildClassPath(String rawcp) throws IOException {
        List<File> result = new ArrayList<File>();
        String[] cp = rawcp.split(":");
        for (String entry : cp) {
            File entryFile;
            if (entry.endsWith("/.")) {
                entryFile = new File(entry.substring(0, entry.length() - 2));
                File[] files = entryFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        result.add(file);
                    }
                }
            } else {
                entryFile = new File(entry);
                result.add(entryFile);
            }
        }
        return result;
    }

}
