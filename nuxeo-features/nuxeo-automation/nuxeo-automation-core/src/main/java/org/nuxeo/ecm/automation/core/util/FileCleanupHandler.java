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
package org.nuxeo.ecm.automation.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.automation.CleanupHandler;

/**
 * Cleanup Handler that takes a list of files and remove them after the operation chain was executed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileCleanupHandler implements CleanupHandler {

    protected List<File> files;

    public FileCleanupHandler() {
        files = new ArrayList<>();
    }

    public FileCleanupHandler(File file) {
        this();
        files.add(file);
    }

    public FileCleanupHandler(Collection<File> files) {
        this();
        this.files.addAll(files);
    }

    @Override
    public void cleanup() {
        for (File file : files) {
            file.delete();
        }
    }

}
