/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.platform.scanimporter.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public abstract class ImportTestCase extends SQLRepositoryTestCase {

    protected List<File> tmp = new ArrayList<File>();

    protected String deployTestFiles(String name) throws IOException {
        File src = new File(
                org.nuxeo.common.utils.FileUtils.getResourcePathFromContext("data/"
                        + name));
        File dst = File.createTempFile("nuxeoImportTestCase", ".dir");
        dst.delete();
        dst.mkdir();
        Framework.getProperties().put("nuxeo.import.tmpdir", dst.getPath());
        tmp.add(dst);
        FileUtils.copyDirectoryToDirectory(src, dst);
        return dst.getPath() + File.separator + name;
    }

    @After
    @Override
    public void tearDown() throws Exception {
        for (File dir : tmp) {
            if (dir.exists()) {
                FileUtils.deleteDirectory(dir);
            }
        }
        tmp.clear();
        super.tearDown();
    }

}
