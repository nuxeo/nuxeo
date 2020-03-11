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

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("copy")
public class CopyOperation {

    @XNode("@path")
    protected String path;

    @XNode("@target")
    protected String target;

    public void run(Installer installer, File bundleDir, File installDir) throws IOException {
        File dest = new File(installDir, target);
        if (path.endsWith("/*")) {
            dest.mkdirs();
            File file = new File(bundleDir, path.substring(0, path.length() - 1));
            FileUtils.copy(file.listFiles(), dest);
        } else {
            File file = new File(bundleDir, path);
            FileUtils.copy(file, dest);
        }
    }

}
