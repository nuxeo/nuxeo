import java.io.File;
import java.io.IOException;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;

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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Imports {

    public static void main(String[] args) throws Exception {

        File root = new File("/Users/bstefanescu/work/nuxeo/nuxeo-features/nuxeo-automation/nuxeo-automation-server/src/main/java");

        printImports(root);

    }

    public static void printImports(File root) throws IOException {
        File[] files = root.listFiles();
        if (files != null) {
        for (File file : files) {
            if (file.getName().endsWith(".java")) {
                List<String> lines = FileUtils.readLines(file);
                System.out.println(file.getPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    if (line.startsWith("import ")) {
                        System.out.println("\t"+line.substring("import ".length()).trim());
                    }
                    if (line.startsWith("class ") || line.contains("class ")) {
                        break;
                    }
                }
            } else if (file.isDirectory()) {
                printImports(file);
            }
        }
        }
    }
}
