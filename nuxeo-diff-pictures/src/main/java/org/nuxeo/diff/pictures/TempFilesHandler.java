/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.diff.pictures;

import java.io.File;

/**
 * 
 *
 * @since 7.3
 */
public class TempFilesHandler {

    protected static final String TEMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    public static File prepareOrGetTempFolder(String leftDocId, String rightDocId) {
        
        String path = TEMP_DIR_PATH + "/" + leftDocId + rightDocId;
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdir();
        }

        return folder;

    }

    public static void deleteTempFolder(String leftDocId, String rightDocId) {

        File folder = prepareOrGetTempFolder(leftDocId, rightDocId);
        if (folder != null && folder.exists() && folder.isDirectory()) {
            for (File f : folder.listFiles()) {
                f.delete();
            }
            folder.delete();
        }

    }

}
