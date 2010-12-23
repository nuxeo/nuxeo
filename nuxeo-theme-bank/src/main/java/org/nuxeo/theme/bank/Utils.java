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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.presets.PaletteIdentifyException;
import org.nuxeo.theme.presets.PaletteParseException;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.resources.BankManager;
import org.nuxeo.theme.resources.BankUtils;

public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);

    private static final List<String> PRESET_CATEGORIES = Arrays.asList(
            "color", "background", "font", "border");

    public static List<String> getCollections(String bankName)
            throws IOException {
        return BankManager.getCollections(bankName);
    }

    public static List<String> getItemsInCollection(String bankName,
            String collection, String typeName) throws IOException {
        return BankManager.getItemsInCollection(bankName, collection, typeName);
    }

    public static List<String> listSkinsInCollection(String bankName,
            String collection) throws IOException {
        Map<String, Object> info;
        info = BankManager.getInfo(bankName, collection, "style");
        List<String> skins = new ArrayList<String>();
        if (info != null) {
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                String resource = entry.getKey();
                Map value = (Map) entry.getValue();
                Boolean isSkin = false;
                if (value.containsKey("skin")) {
                    isSkin = (Boolean) value.get("skin");
                }
                if (isSkin) {
                    skins.add(resource);
                }
            }
        }
        return skins;
    }

    public static Properties getPresetProperties(String bank,
            String collection, String category) {
        String path = String.format("%s/%s/preset/%s", bank, collection,
                category);
        File file;
        try {
            file = BankManager.getFile(path);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        Properties properties = new Properties();
        if (!file.exists()) {
            return properties;
        }
        for (File f : file.listFiles()) {
            String content;
            try {
                content = BankUtils.getFileContent(f);
            } catch (IOException e) {
                log.warn("Could not read file: " + f.getAbsolutePath());
                continue;
            }
            try {
                properties.putAll(PaletteParser.parse(content.getBytes(),
                        f.getName()));
            } catch (PaletteIdentifyException e) {
                log.warn("Could not identify palette type: "
                        + f.getAbsolutePath());
            } catch (PaletteParseException e) {
                log.warn("Could not parse palette: " + f.getAbsolutePath());
            }
        }
        return properties;
    }

    /*
     * JSON calls
     */
    public static String listBankSkins(String bankName) throws IOException {
        JSONArray skins = new JSONArray();
        for (String collection : BankManager.getCollections(bankName)) {
            Map<String, Object> info = BankManager.getInfo(bankName,
                    collection, "style");
            if (info == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                String resource = entry.getKey();
                Map value = (Map) entry.getValue();
                if (value.containsKey("skin") && (Boolean) value.get("skin")) {
                    Boolean isBase = false;
                    if (value.containsKey("base")) {
                        isBase = (Boolean) value.get("base");
                    }
                    JSONObject skinMap = new JSONObject();
                    skinMap.put("bank", bankName);
                    skinMap.put("collection", collection);
                    skinMap.put("resource", resource);
                    skinMap.put("name", String.format("%s (%s)",
                            resource.replace(".css", ""), collection));
                    skinMap.put("base", isBase);
                    skins.add(skinMap);
                }
            }
        }
        return skins.toString();
    }

    public static String listBankStyles(String bankName) throws IOException {
        JSONArray styles = new JSONArray();
        for (String collection : BankManager.getCollections(bankName)) {
            for (String resource : getItemsInCollection(bankName, collection,
                    "style")) {
                JSONObject styleMap = new JSONObject();
                styleMap.put("bank", bankName);
                styleMap.put("collection", collection);
                styleMap.put("resource", resource);
                styleMap.put("name", String.format("%s (%s)", resource.replace(
                        ".css", ""), collection));
                styles.add(styleMap);
            }
        }
        return styles.toString();
    }

    public static String listBankPresets(String bankName) throws IOException {
        JSONArray presets = new JSONArray();
        for (String collection : BankManager.getCollections(bankName)) {
            for (String category : PRESET_CATEGORIES) {
                for (Map.Entry property : getPresetProperties(bankName,
                        collection, category).entrySet()) {
                    JSONObject presetMap = new JSONObject();
                    presetMap.put("bank", bankName);
                    presetMap.put("collection", collection);
                    presetMap.put("category", category);
                    presetMap.put("name", property.getKey());
                    presetMap.put("value", property.getValue());
                    presets.add(presetMap);
                }
            }
        }
        return presets.toString();
    }

    public static String listImages(String bank) throws IOException {
        JSONArray index = new JSONArray();
        for (String collection : BankManager.getCollections(bank)) {
            String path = String.format("%s/%s/image/", bank, collection);
            File file = BankManager.getFile(path);
            if (!file.isDirectory()) {
                throw new IOException("Expected folder: " + path);
            }
            for (File image : file.listFiles()) {
                JSONObject imageMap = new JSONObject();
                imageMap.put("name", image.getName());
                imageMap.put("collection", collection);
                index.add(imageMap);
            }
        }
        return index.toString();
    }

    public static String listCollections(String bank) throws IOException {
        JSONArray index = new JSONArray();
        for (String collection : BankManager.getCollections(bank)) {
            index.add(collection);
        }
        return index.toString();
    }

    public static String getNavTree() throws IOException {
        JSONArray tree = new JSONArray();

        for (String bankName : BankManager.getBankNames()) {
            JSONObject bankNode = new JSONObject();
            bankNode.put("state", "open");

            JSONObject bankMap = new JSONObject();
            bankMap.put("title", bankName);

            JSONObject bankAttributes = new JSONObject();
            bankAttributes.put("rel", "bank");
            bankAttributes.put("path", String.format("/%s", bankName));
            bankAttributes.put("id", BankUtils.getDomId(bankName));
            bankNode.put("attributes", bankAttributes);
            bankNode.put("data", bankMap);

            JSONArray childrenNodes = new JSONArray();
            for (String collection : BankManager.getCollections(bankName)) {
                childrenNodes.add(getNavTreeCollectionNode(bankName, collection));
            }
            bankNode.put("children", childrenNodes);

            tree.add(bankNode);
        }
        return tree.toString();
    }

    private static JSONObject getNavTreeCollectionNode(String bankName,
            String collection) throws IOException {

        JSONObject collectionNode = new JSONObject();

        JSONObject collectionMap = new JSONObject();
        collectionMap.put("title", collection);

        JSONObject collectionAttributes = new JSONObject();
        collectionAttributes.put("rel", "collection");
        collectionAttributes.put("path", String.format("/%s/%s", bankName,
                collection));
        collectionAttributes.put("id", BankUtils.getDomId(String.format(
                "%s-%s", bankName, collection)));

        collectionNode.put("data", collectionMap);
        collectionNode.put("attributes", collectionAttributes);

        JSONArray folderTypeNodes = new JSONArray();
        final String[] TYPE_NAMES = { "skin", "style", "preset", "image" };
        for (String typeName : TYPE_NAMES) {
            JSONObject folderTypeNode = new JSONObject();
            JSONObject folderTypeMap = new JSONObject();
            folderTypeMap.put("title", typeName);

            JSONObject folderTypeAttributes = new JSONObject();
            folderTypeAttributes.put("rel", "folder");
            folderTypeAttributes.put("path", String.format("/%s/%s/%s",
                    bankName, collection, typeName));
            folderTypeAttributes.put("id", BankUtils.getDomId(String.format(
                    "%s-%s-%s", bankName, collection, typeName)));

            folderTypeNode.put("attributes", folderTypeAttributes);
            folderTypeNode.put("data", folderTypeMap);

            JSONArray items = new JSONArray();
            List<String> skins = listSkinsInCollection(bankName, collection);
            String effectiveTypeName = "skin".equals(typeName) ? "style"
                    : typeName;
            for (String item : BankManager.getItemsInCollection(bankName,
                    collection, effectiveTypeName)) {

                if ("skin".equals(typeName)) {
                    if (!skins.contains(item)) {
                        continue;
                    }
                } else if ("style".equals(typeName)) {
                    if (skins.contains(item)) {
                        continue;
                    }
                }
                JSONObject itemNode = new JSONObject();
                JSONObject itemMap = new JSONObject();
                itemMap.put("title", item);

                JSONObject itemAttributes = new JSONObject();
                itemAttributes.put("rel", typeName);
                itemAttributes.put("path", String.format("/%s/%s/%s/%s",
                        bankName, collection, typeName, item));
                itemAttributes.put("id", BankUtils.getDomId(String.format(
                        "%s-%s-%s-%s", bankName, collection, typeName, item)));
                itemNode.put("attributes", itemAttributes);
                itemNode.put("data", itemMap);

                items.add(itemNode);
            }
            folderTypeNode.put("children", items);
            folderTypeNodes.add(folderTypeNode);
        }
        collectionNode.put("children", folderTypeNodes);
        return collectionNode;
    }

    /*
     * IO
     */
    public static StreamingOutput streamFile(final File file) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException,
                    WebApplicationException {
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    IOUtils.copy(in, out);
                } catch (FileNotFoundException e) {
                    throw new WebApplicationException(e,
                            Response.Status.NOT_FOUND);
                } catch (Exception e) {
                    throw new WebApplicationException(e,
                            Response.Status.INTERNAL_SERVER_ERROR);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        };
    }

}
