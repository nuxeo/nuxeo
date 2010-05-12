/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.theme.bank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.api.Framework;

public class BankManager {
    private static final File BANKS_DIR;

    static {
        BANKS_DIR = new File(Framework.getRuntime().getHome(), "theme-banks");
        BANKS_DIR.mkdirs();
    }

    public static File getFile(String path) {
        return new File(BANKS_DIR, path);
    }

    public static File getBankDir(String bankName) {
        return getFile(bankName);
    }

    public static List<String> getBankNames() {
        List<String> names = new ArrayList<String>();
        for (String bankName : BANKS_DIR.list()) {
            names.add(bankName);
        }
        return names;
    }

    /*
     * Collections
     */
    public static List<String> getCollections(String bank, String typeName) {
        List<String> names = new ArrayList<String>();
        String path = String.format("%s/%s", bank, typeName);
        File file = BankManager.getFile(path);
        for (String collectionName : file.list()) {
            names.add(collectionName);
        }
        return names;
    }

    public static List<String> getItemsInCollection(String bank,
            String typeName, String collection) {
        List<String> names = new ArrayList<String>();
        String path = String.format("%s/%s/%s", bank, typeName, collection);
        File file = BankManager.getFile(path);
        for (String item : file.list()) {
            names.add(item);
        }
        return names;
    }

    /*
     * I/O
     */
    public static void importBankData(String bankName, URL srcFileUrl) {
        InputStream in = null;
        try {
            in = srcFileUrl.openStream();
            String prefix = bankName;
            ZipUtils.unzip(prefix, in, getBankDir(bankName));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {

                }
            }
        }

    }

}
