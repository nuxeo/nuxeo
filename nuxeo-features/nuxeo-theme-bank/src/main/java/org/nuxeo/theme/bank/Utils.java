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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.resources.BankManager;
import org.nuxeo.theme.resources.BankUtils;

public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);

    public static List<String> listSkinsInCollection(String bankName,
            String collection) {
        Map<String, Object> info;
        try {
            info = BankManager.getInfo(bankName, collection, "style");
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
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

    /*
     * JSON calls
     */
    public static String listBankSkins(String bankName) {
        JSONArray skins = new JSONArray();
        try {
            for (String collection : BankManager.getCollections(bankName)) {
                Map<String, Object> info = BankManager.getInfo(bankName,
                        collection, "style");
                if (info == null) {
                    continue;
                }
                for (Map.Entry<String, Object> entry : info.entrySet()) {
                    String resource = entry.getKey();
                    Map value = (Map) entry.getValue();
                    Boolean isSkin = false;
                    if (value.containsKey("skin")) {
                        isSkin = (Boolean) value.get("skin");
                    }
                    if (isSkin) {
                        JSONObject skinMap = new JSONObject();
                        skinMap.put("bank", bankName);
                        skinMap.put("collection", collection);
                        skinMap.put("resource", resource);
                        skinMap.put("name", String.format("%s (%s)",
                                resource.replace(".css", ""), collection));
                        skins.add(skinMap);
                    }
                }
            }
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        return skins.toString();
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
                index.add(String.format("%s/%s", collection, image.getName()));
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
        collectionAttributes.put("path",
                String.format("/%s/%s", bankName, collection));
        collectionAttributes.put(
                "id",
                BankUtils.getDomId(String.format("%s-%s", bankName, collection)));

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
            folderTypeAttributes.put("path",
                    String.format("/%s/%s/%s", bankName, collection, typeName));
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
