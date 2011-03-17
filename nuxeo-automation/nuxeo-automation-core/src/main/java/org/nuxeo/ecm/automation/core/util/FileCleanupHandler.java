/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Cleanup Handler that takes a list of files and remove them after the
 * operation chain was executed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileCleanupHandler implements CleanupHandler {

    protected List<File> files;

    public FileCleanupHandler() {
        files = new ArrayList<File>();
    }

    public FileCleanupHandler(File file) {
        this();
        files.add(file);
    }

    public FileCleanupHandler(Collection<File> files) {
        this();
        this.files.addAll(files);
    }

    public void cleanup() throws Exception {
        for (File file : files) {
            file.delete();
        }
    }

}
