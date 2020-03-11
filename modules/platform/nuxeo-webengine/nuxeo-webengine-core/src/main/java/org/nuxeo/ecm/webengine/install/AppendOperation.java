/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.install;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("append")
public class AppendOperation {

    @XNode("@path")
    protected String path;

    @XNode("@target")
    protected String target;

    @XNode("@appendNewLine")
    protected boolean appendNewLine = true;

    public void run(Installer installer, File bundleDir, File installDir) throws IOException {
        // ctx.getBundle().getEntryPaths(path);
        File src = new File(bundleDir, path);
        if (src.isFile()) {
            String text = FileUtils.readFileToString(src, UTF_8);
            if (appendNewLine) {
                String crlf = System.lineSeparator();
                text = crlf + text + crlf;
            }
            File file = new File(installDir, target);
            File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }
            boolean append = file.exists();
            try (FileOutputStream out = new FileOutputStream(file, append)) {
                out.write(text.getBytes());
            }
        } else {
            installer.logWarning("Could not find path: " + path + " to append");
        }
    }

}
