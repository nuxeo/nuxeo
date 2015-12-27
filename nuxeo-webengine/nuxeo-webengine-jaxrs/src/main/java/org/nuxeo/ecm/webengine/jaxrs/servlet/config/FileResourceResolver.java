/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileResourceResolver implements ResourceResolver {

    protected File file;

    public FileResourceResolver(String path) {
        file = new File(Framework.expandVars(path));
    }

    public FileResourceResolver(File file) {
        this.file = file;
    }

    @Override
    public URL getResource(String name) {
        File f = new File(file, name);
        if (f.isFile()) {
            try {
                return f.toURI().toURL();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}
