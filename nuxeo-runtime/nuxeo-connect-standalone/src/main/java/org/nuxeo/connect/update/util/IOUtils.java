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
package org.nuxeo.connect.update.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IOUtils {

    private IOUtils() {
    }

    /**
     * Backup the given file to the package backup directory. The backup file will be returned. The backup file will be
     * named: MD5ofFilepath_filename.
     *
     * @return the name of the backup file.
     */
    public static File backup(LocalPackage pkg, File file) throws IOException {
        file = file.getCanonicalFile();
        String md5 = createMd5(file.getAbsolutePath());
        File bak = pkg.getData().getEntry(LocalPackage.BACKUP_DIR);
        bak.mkdirs();
        String name = file.getName();
        File bakFile = new File(bak, md5 + "_" + name);
        FileUtils.copy(file, bakFile);
        return bakFile;
    }

    public static String createMd5(String text) throws IOException {
        MessageDigest digest = getMD5Digest();
        digest.update(text.getBytes());
        byte[] hash = digest.digest();
        return md5ToHex(hash);
    }

    public static String createMd5(File file) throws IOException {
        MessageDigest digest = getMD5Digest();
        FileInputStream in = new FileInputStream(file);
        try {
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
        } finally {
            in.close();
        }
    }

    protected static MessageDigest getMD5Digest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
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
