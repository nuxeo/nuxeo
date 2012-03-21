/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IOUtils {

    private IOUtils() {
    }

    /**
     * Backup the given file to the package backup directory. The backup file
     * will be returned. The backup file will be named: MD5ofFilepath_filename.
     *
     * @return the name of the backup file.
     */
    public static File backup(LocalPackage pkg, File file) throws Exception {
        file = file.getCanonicalFile();
        String md5 = createMd5(file.getAbsolutePath());
        File bak = pkg.getData().getEntry(LocalPackage.BACKUP_DIR);
        bak.mkdirs();
        String name = file.getName();
        File bakFile = new File(bak, md5 + "_" + name);
        FileUtils.copy(file, bakFile);
        return bakFile;
    }

    public static String createMd5(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(text.getBytes());
        byte[] hash = digest.digest();
        return md5ToHex(hash);
    }

    public static String createMd5(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream in = new FileInputStream(file);
        try{
            byte[] bytes = new byte[64 * 1024];
            int r = in.read(bytes);
            while (r > -1) {
                if (r > 0) {
                    digest.update(bytes, 0, r);
                }
                r = in.read(bytes);
            }
            byte[] hash = digest.digest();
            return md5ToHex(hash);
        }finally{
            in.close();
        }
    }

    public static String md5ToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
