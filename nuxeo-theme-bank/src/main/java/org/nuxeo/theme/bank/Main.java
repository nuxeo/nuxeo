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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.nuxeo.theme.presets.PaletteParseException;
import org.nuxeo.theme.presets.PaletteIdentifyException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.theme.presets.PaletteParser;

@WebObject(type = "theme-banks")
@Produces(MediaType.TEXT_HTML)
public class Main extends ModuleRoot {

    private static final String SERVER_ID = "Nuxeo/ThemeBank-1.0";

    @GET
    public Object getIndex() {
        return getTemplate("index.ftl");
    }

    /*
     * Banks
     */
    @GET
    @Path("{bank}")
    public Object displayBank(@PathParam("bank") String bank) {
        return getTemplate("bank.ftl").arg("bank", bank);
    }
    
    @GET
    @Path("{bank}/logo")
    public Object displayBankThumbnail(@PathParam("bank") String bank) {
        File file = BankManager.getBankLogoFile(bank);
        if (!file.exists()) {
            return Response.status(404).build();
        }
        String ext = FileUtils.getFileExtension(path);
        String mimeType = ctx.getEngine().getMimeType(ext);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return Response.ok().entity(streamFile(file)).lastModified(
                new Date(file.lastModified())).header("Cache-Control", "public").header(
                "Server", SERVER_ID).type(mimeType).build();
    }

    @GET
    @Path("{bank}/navtree")
    public Object getNavtreeView(@PathParam("bank") String bank) {
        return getTemplate("navtree.ftl").arg("bank", bank);
    }
    
    /*
     * Styles
     */
    @GET
    @Path("{bank}/style/{collection}/view")
    public Object getStyleCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("styleCollection.ftl").arg("styles",
                BankManager.getItemsInCollection(bank, "style", collection)).arg(
                "collection", collection).arg("bank", bank);
    }

    @GET
    @Produces("text/css")
    @Path("{bank}/style/{collection}/{resource}")
    public Response getStyle(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file = BankManager.getStyleFile(bank, collection, resource);
        return Response.ok().entity(streamFile(file)).lastModified(
                new Date(file.lastModified())).header("Cache-Control", "public").header(
                "Server", SERVER_ID).build();
    }

    @GET
    @Path("{bank}/style/{collection}/{resource}/view")
    public Object renderStyleView(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file = BankManager.getStyleFile(bank, collection, resource);
        String content = BankUtils.getFileContent(file);
        return getTemplate("style.ftl").arg("content", content).arg("resource", resource).arg("collection", collection);
    }

    /*
     * Presets
     */
    @GET
    @Path("{bank}/preset/{collection}/view")
    public Object getPresetCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("presetCollection.ftl").arg("presets",
                BankManager.getItemsInCollection(bank, "preset", collection)).arg(
                "collection", collection).arg("bank", bank);
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{bank}/preset/{collection}/{category}")
    public Response getPreset(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("category") String category) {
        String path = String.format("%s/preset/%s/%s", bank, collection,
                category);
        File file = BankManager.getFile(path);
        String content = "";
        StringBuilder sb = new StringBuilder();
        for (File f : file.listFiles()) {
            content = BankUtils.getFileContent(f);
            content = PaletteParser.renderPaletteAsCsv(content.getBytes(),
                    f.getName());
            sb.append(content);
        }
        content = sb.toString();

        return Response.ok(content).lastModified(new Date(file.lastModified())).header(
                "Cache-Control", "public").header("Server", SERVER_ID).build();
    }

    @GET
    @Path("{bank}/preset/{collection}/{category}/view")
    public Object getPresetView(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("category") String category) {
        String path = String.format("%s/preset/%s/%s", bank, collection,
                category);
        File file = BankManager.getFile(path);
        Properties properties = new Properties();
        for (File f : file.listFiles()) {
            String content = BankUtils.getFileContent(f);
            try {
                properties.putAll(PaletteParser.parse(content.getBytes(), f.getName()));
            } catch (PaletteIdentifyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (PaletteParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return getTemplate("preset.ftl").arg("properties", properties).arg("collection", collection).arg("category", category);
    }

    /*
     * Images
     */
    @GET
    @Path("{bank}/image/{collection}/view")
    public Object getImageCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("imageCollection.ftl").arg("images",
                BankManager.getItemsInCollection(bank, "image", collection)).arg(
                "collection", collection).arg("bank", bank);
    }

    @GET
    @Path("{bank}/image/{collection}/{resource}")
    public Response getImage(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file = BankManager.getImageFile(bank, collection, resource);
        if (!file.exists()) {
            return Response.status(404).build();
        }
        String ext = FileUtils.getFileExtension(path);
        String mimeType = ctx.getEngine().getMimeType(ext);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return Response.ok().entity(streamFile(file)).lastModified(
                new Date(file.lastModified())).header("Cache-Control", "public").header(
                "Server", SERVER_ID).type(mimeType).build();
    }

    @GET
    @Path("{bank}/image/{collection}/{resource}/view")
    public Object getImageView(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        String path = String.format("%s/image/%s/%s", bank, collection,
                resource);
        return getTemplate("image.ftl").arg("path", path).arg("resource", resource).arg("collection", collection);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bank}/json/images")
    public String listImages(@PathParam("bank") String bank) {
        String path = String.format("%s/image/", bank);
        JSONArray index = new JSONArray();
        File file = BankManager.getFile(path);
        for (File c : file.listFiles()) {
            if (!c.isDirectory()) {
                continue;
            }
            for (File i : c.listFiles()) {
                index.add(String.format("%s/%s", c.getName(), i.getName()));
            }
        }
        return index.toString();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bank}/json/tree")
    public String getTree(@PathParam("bank") String bankName) {
        JSONArray tree = new JSONArray();

        JSONObject bankNode = new JSONObject();
        bankNode.put("state", "open");

        JSONObject bankMap = new JSONObject();
        bankMap.put("title", bankName);

        JSONObject bankAttributes = new JSONObject();
        bankAttributes.put("rel", "bank");
        bankNode.put("attributes", bankAttributes);
        bankNode.put("data", bankMap);

        JSONArray folderTypes = new JSONArray();
        folderTypes.add(getNavTreeNode(bankName, "style"));
        folderTypes.add(getNavTreeNode(bankName, "preset"));
        folderTypes.add(getNavTreeNode(bankName, "image"));
        bankNode.put("children", folderTypes);

        tree.add(bankNode);
        return tree.toString();
    }

    private JSONObject getNavTreeNode(String bankName, String typeName) {

        JSONObject folderTypeNode = new JSONObject();

        JSONObject folderTypeMap = new JSONObject();
        folderTypeMap.put("title", typeName);

        JSONObject folderTypeAttributes = new JSONObject();
        folderTypeAttributes.put("rel", "folder");
        folderTypeAttributes.put("path", String.format("/%s/%s", bankName,
                typeName));
        folderTypeNode.put("attributes", folderTypeAttributes);
        folderTypeNode.put("data", folderTypeMap);

        JSONArray collections = new JSONArray();
        for (String c : getCollections(bankName, typeName)) {
            JSONArray items = new JSONArray();

            JSONObject collectionNode = new JSONObject();
            JSONObject collectionMap = new JSONObject();
            collectionMap.put("title", c);

            JSONObject collectionAttributes = new JSONObject();
            collectionAttributes.put("rel", "collection");
            collectionAttributes.put("path", String.format("/%s/%s/%s",
                    bankName, typeName, c));
            collectionNode.put("attributes", collectionAttributes);
            collectionNode.put("data", collectionMap);

            for (String item : getItemsInCollection(bankName, typeName, c)) {

                JSONObject itemNode = new JSONObject();

                JSONObject itemMap = new JSONObject();
                itemMap.put("title", item);

                JSONObject itemAttributes = new JSONObject();
                itemAttributes.put("rel", typeName);
                itemAttributes.put("path", String.format("/%s/%s/%s/%s",
                        bankName, typeName, c, item));

                itemNode.put("attributes", itemAttributes);
                itemNode.put("data", itemMap);
                items.add(itemNode);
            }
            collectionNode.put("children", items);
            collections.add(collectionNode);
        }
        folderTypeNode.put("children", collections);
        return folderTypeNode;
    }

    /*
     * API
     */
    public static List<String> getBankNames() {
        return BankManager.getBankNames();
    }

    public static List<String> getCollections(String bank, String typeName) {
        return BankManager.getCollections(bank, typeName);
    }

    public static List<String> getItemsInCollection(String bank,
            String typeName, String collection) {
        return BankManager.getItemsInCollection(bank, typeName, collection);
    }

    private static StreamingOutput streamFile(final File file) {
        return new StreamingOutput() {
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
