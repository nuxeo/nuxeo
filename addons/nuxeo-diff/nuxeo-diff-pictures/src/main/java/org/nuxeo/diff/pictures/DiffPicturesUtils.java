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

import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 7.4
 */
public class DiffPicturesUtils {

    /*
     * Centralize handling of the targetFileName (used in at least 3 operations => less code in the operation itself)
     */
    public static String updateTargetFileName(Blob inBlob, String inTargetFileName, String inTargetFileSuffix) {

        String updatedName = "";

        if (inTargetFileName == null || inTargetFileName.isEmpty()) {
            updatedName = inBlob.getFilename();
        } else {
            updatedName = inTargetFileName;
        }

        if (inTargetFileSuffix != null && !inTargetFileSuffix.isEmpty()) {
            updatedName = DiffPicturesUtils.addSuffixToFileName(updatedName, inTargetFileSuffix);
        }

        return updatedName;
    }

    /*
     * Adds the suffix before the file extension, if any
     */
    public static String addSuffixToFileName(String inFileName, String inSuffix) {
        if (inFileName == null || inFileName.isEmpty() || inSuffix == null || inSuffix.isEmpty()) {
            return inFileName;
        }

        int dotIndex = inFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return inFileName + inSuffix;
        }

        return inFileName.substring(0, dotIndex) + inSuffix + inFileName.substring(dotIndex);
    }

}
