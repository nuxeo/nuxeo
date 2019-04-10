/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to compute a unique id for an import task.
 *
 * @since 5.7.3
 */
public class CSVImportId {

    private static final Log log = LogFactory.getLog(CSVImportId.class);

    private CSVImportId() {
        // utility class
    }

    public static String create(String repositoryName, String path, File csvFile) {
        return create(repositoryName, path, computeDigest(csvFile));
    }

    public static String create(String repositoryName, String path, String csvBlobDigest) {
        return repositoryName + ':' + path + ":csvImport:" + csvBlobDigest;
    }

    protected static String computeDigest(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return DigestUtils.md5Hex(in);
        } catch (IOException e) {
            log.error(e, e);
            return "";
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}