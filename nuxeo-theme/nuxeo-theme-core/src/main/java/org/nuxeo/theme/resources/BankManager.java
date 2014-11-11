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

package org.nuxeo.theme.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.theme.themes.ThemeManager;
import org.yaml.snakeyaml.Yaml;

public class BankManager {
    private static final Log log = LogFactory.getLog(BankManager.class);

    private static File BANKS_DIR;

    private static List<String> FOLDER_NAMES = Arrays.asList("style", "preset",
            "image");

    private static final String CUSTOM_COLLECTION_DIRNAME = "custom";

    public static File getBanksDir() {
        if (BANKS_DIR == null) {
            BANKS_DIR = new File(ThemeManager.getThemeDir(), "banks");
            BANKS_DIR.mkdirs();
        }
        return BANKS_DIR;
    }

    public static File getFile(String path) throws IOException {
        if (!BankUtils.checkFilePath(path)) {
            throw new IOException("File path not allowed: " + path);
        }
        return new File(getBanksDir(), path);
    }

    public static File getBankDir(String bankName) throws IOException {
        return getFile(bankName);
    }

    public static List<String> getBankNames() {
        List<String> names = new ArrayList<String>();
        for (File bankFile : BankUtils.listFilesSorted(getBanksDir())) {
            names.add(bankFile.getName());
        }
        return names;
    }

    public static void setupBanks() {
        for (String bankName : getBankNames()) {
            try {
                setupBankStructure(bankName);
            } catch (IOException e) {
                log.error("Could not create bank structure: " + bankName, e);
            }
        }
    }

    public static void setupBankStructure(String bankName) throws IOException {
        for (String folderName : FOLDER_NAMES) {
            String folderPath = String.format("/%s/%s",
                    CUSTOM_COLLECTION_DIRNAME, folderName);
            createFolder(bankName, folderPath);
        }
    }

    /*
     * Collections
     */
    public static List<String> getCollections(String bank) throws IOException {
        List<String> names = new ArrayList<String>();
        File file = getBankDir(bank);
        if (file.exists()) {
            for (File collectionFile : BankUtils.listFilesSorted(file)) {
                names.add(collectionFile.getName());
            }
        }
        return names;
    }

    public static List<String> getItemsInCollection(String bank,
            String collection, String typeName) throws IOException {
        List<String> names = new ArrayList<String>();
        String path = String.format("%s/%s/%s", bank, collection, typeName);
        File file = getFile(path);
        if (file.exists()) {
            for (File item : BankUtils.listFilesSorted(file)) {
                String itemName = item.getName();
                if (typeName.equals("style") && !itemName.endsWith(".css")) {
                    continue;
                }
                names.add(itemName);
            }
        }
        return names;
    }

    public static File getStyleFile(String bank, String collection,
            String resource) throws IOException {
        String path = String.format("%s/%s/style/%s", bank, collection,
                resource);
        return getFile(path);
    }

    public static File getImageFile(String bank, String collection,
            String resource) throws IOException {
        String path = String.format("%s/%s/image/%s", bank, collection,
                resource);
        return getFile(path);
    }

    public static File getBankLogoFile(String bank) throws IOException {
        String path = String.format("%s/logo.png", bank);
        return getFile(path);
    }

    @SuppressWarnings("rawtypes")
    public static File getStylePreviewFile(String bank, String collection,
            String resource) throws IOException {
        Map<String, Object> info = getInfo(bank, collection, "style");

        if (!info.containsKey(resource)) {
            throw new IOException("Style preview not found: " + resource);
        }

        Map value = (Map) info.get(resource);
        if (!value.containsKey("preview")) {
            throw new IOException("Style preview not found: " + resource);
        }

        String preview = (String) value.get("preview");
        String path = String.format("%s/%s/style/%s", bank, collection, preview);

        File file = getFile(path);
        if (!file.exists()) {
            throw new IOException("Style preview not found: " + resource);
        }
        return file;
    }

    public static File getInfoFile(String bank, String collection,
            String typeName) throws IOException {
        String path = String.format("%s/%s/%s/info.txt", bank, collection,
                typeName);
        return getFile(path);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getInfo(String bank, String collection,
            String typeName) throws IOException {
        File file = getInfoFile(bank, collection, typeName);
        Yaml yaml = new Yaml();
        String content = "";
        if (file.exists()) {
            content = BankUtils.getFileContent(file);
        }
        return (Map<String, Object>) yaml.load(content);
    }

    /*
     * I/O
     */
    public static void importBankData(String bankName, String collection,
            URL srcFileUrl) throws IOException {
        if (CUSTOM_COLLECTION_DIRNAME.equals(collection)) {
            throw new IOException("Bank collection name not allowed: "
                    + CUSTOM_COLLECTION_DIRNAME);
        }
        InputStream in = null;
        in = srcFileUrl.openStream();
        String path = String.format("%s/%s", bankName, collection);
        File folder = getFile(path);
        if (folder.exists()) {
            FileUtils.deleteTree(folder);
        }
        folder.mkdir();
        ZipUtils.unzip(in, folder);
        if (in != null) {
            in.close();
        }
    }

    public static byte[] exportBankData(String bankName, String collection)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String path = String.format("%s/%s", bankName, collection);
        File folder = getFile(path);
        if (!folder.exists()) {
            throw new IOException("Folder not found: " + path);
        }
        ZipUtils.zip(folder.listFiles(), out, null);
        out.close();
        return out.toByteArray();
    }

    public static File createFolder(String path, String folderName)
            throws IOException {
        String folderPath = String.format("%s/%s", path, folderName);
        File file = getFile(folderPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public static File createFile(String path, String fileName, String content)
            throws IOException {
        return createFile(path, fileName, content.getBytes());
    }

    public static File createFile(String path, String fileName, byte[] data)
            throws IOException {
        File file = new File(getFile(path), fileName);
        file.createNewFile();
        FileUtils.writeFile(file, data);
        return file;
    }

    public static void editFile(String path, String fileName, String content)
            throws IOException {
        File file = new File(getFile(path), fileName);
        FileUtils.writeFile(file, content);
    }
}
