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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.theme.presets.PaletteIdentifyException;
import org.nuxeo.theme.presets.PaletteParseException;
import org.nuxeo.theme.presets.PaletteParser;
import org.nuxeo.theme.resources.BankManager;
import org.nuxeo.theme.resources.BankUtils;

@WebObject(type = "theme-banks")
@Produces(MediaType.TEXT_HTML)
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    private static final String SERVER_ID = "Nuxeo/ThemeBank-1.0";

    @GET
    public Object getIndex() {
        return getTemplate("index.ftl");
    }

    /*
     * Management mode
     */
    @Path("{bank}/manage")
    public Object getManagement(@PathParam("bank") String bank) {
        return newObject("Management", bank);
    }

    /*
     * Banks
     */
    @GET
    @Path("{bank}")
    public Object displayBank(@PathParam("bank") String bank) {
        return getTemplate("index.ftl").arg("bank", bank);
    }

    @GET
    @Path("{bank}/view")
    public Object displayBankView(@PathParam("bank") String bank) {
        return getTemplate("bank.ftl").arg("bank", bank);
    }

    @GET
    @Path("{bank}/info")
    public Object getBankInfo(@PathParam("bank") String bank) {
        return "XXX";
    }

    @GET
    @Path("{bank}/logo")
    public Object displayBankLogo(@PathParam("bank") String bank) {
        File file;
        try {
            file = BankManager.getBankLogoFile(bank);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        if (file == null || !file.exists()) {
            return noPreview();
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

    /*
     * UI
     */
    @GET
    @Path("navtree")
    public Object getNavtreeView() {
        return getTemplate("navtree.ftl");
    }

    @GET
    @Path("actionbar")
    public Object getActionBarView() {
        return getTemplate("actionbar.ftl");
    }

    @GET
    @Path("banks")
    public Object getBanksView() {
        return getTemplate("banks.ftl");
    }

    @GET
    @Path("session/login")
    public Object getSessionView() {
        return getTemplate("session.ftl");
    }

    @GET
    @Path("session")
    public Object doSession() {
        Object failed = ctx.getProperty("failed");
        if (failed == null) {
            return getIndex();
        }
        return getSessionView();
    }

    @POST
    @Path("session/@@login")
    public Object login() {
        return getIndex();
    }

    /*
     * Styles
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bank}/json/skins")
    @SuppressWarnings("rawtypes")
    public String listBankSkins(@PathParam("bank") String bankName) {
        JSONArray skins = new JSONArray();
        try {
            for (String collection : BankManager.getCollections(bankName,
                    "style")) {
                Map<String, Object> info = BankManager.getInfo(bankName,
                        "style", collection);
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

    @GET
    @Path("{bank}/skins/view")
    public Object getBankSkinsView(@PathParam("bank") String bank) {
        return getTemplate("skins.ftl").arg("bank", bank);
    }

    public List<String> listSkinsInCollection(String bankName, String collection) {
        Map<String, Object> info;
        try {
            info = BankManager.getInfo(bankName, "style", collection);
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

    @GET
    @Path("{bank}/style/view")
    public Object getStyleCollectionsView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("styleCollections.ftl").arg("collections",
                getCollections(bank, "style")).arg("collection", collection).arg(
                "bank", bank);
    }

    @GET
    @Path("{bank}/style/{collection}/view")
    public Object getStyleCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            Object info = BankManager.getInfo(bank, "style", collection);
            return getTemplate("styleCollection.ftl").arg("info", info).arg(
                    "styles",
                    BankManager.getItemsInCollection(bank, "style", collection)).arg(
                    "collection", collection).arg("bank", bank);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{bank}/style/{collection}/info")
    public Object getStyleCollectionInfo(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            return BankManager.getInfoFile(bank, "style", collection);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Produces("text/css")
    @Path("{bank}/style/{collection}/{resource}")
    public Response getStyle(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file;
        try {
            file = BankManager.getStyleFile(bank, collection, resource);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        return Response.ok().entity(streamFile(file)).lastModified(
                new Date(file.lastModified())).header("Cache-Control", "public").header(
                "Server", SERVER_ID).build();
    }

    @GET
    @Path("{bank}/style/{collection}/{resource}/{action}")
    public Object renderStyle(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource,
            @PathParam("action") String action) {
        File file;
        try {
            file = BankManager.getStyleFile(bank, collection, resource);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        String content;
        try {
            content = BankUtils.getFileContent(file);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        return getTemplate("style.ftl").arg("content", content).arg("bank",
                bank).arg("resource", resource).arg("collection", collection).arg(
                "action", action);
    }

    @GET
    @Path("{bank}/style/{collection}/{resource}/preview")
    public Object displayStylePreview(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file;
        try {
            file = BankManager.getStylePreviewFile(bank, collection, resource);
        } catch (IOException e) {
            return noPreview();
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

    /*
     * Presets
     */
    @GET
    @Path("{bank}/preset/view")
    public Object getPresetCollectionsView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("presetCollections.ftl").arg("collections",
                getCollections(bank, "preset")).arg("collection", collection).arg(
                "bank", bank);
    }

    @GET
    @Path("{bank}/preset/{collection}/view")
    public Object getPresetCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            return getTemplate("presetCollection.ftl").arg(
                    "presets",
                    BankManager.getItemsInCollection(bank, "preset", collection)).arg(
                    "collection", collection).arg("bank", bank);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{bank}/preset/{collection}/info")
    public Object getPresetCollectionInfo(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            return BankManager.getInfoFile(bank, "preset", collection);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{bank}/preset/{collection}/{category}")
    public Response getPreset(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("category") String category) {
        String path = String.format("%s/preset/%s/%s", bank, collection,
                category);
        File file;
        try {
            file = BankManager.getFile(path);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        String content = "";
        if (!file.exists()) {
            return Response.status(404).build();
        }

        StringBuilder sb = new StringBuilder();
        for (File f : file.listFiles()) {
            try {
                content = BankUtils.getFileContent(f);
            } catch (IOException e) {
                log.warn("Could not read file: " + f.getAbsolutePath());
                continue;
            }
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
        File file;
        try {
            file = BankManager.getFile(path);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
        Properties properties = new Properties();
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
        return getTemplate("preset.ftl").arg("properties", properties).arg(
                "bank", bank).arg("collection", collection).arg("category",
                category);
    }

    /*
     * Images
     */
    @GET
    @Path("{bank}/image/view")
    public Object getImageCollectionsView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("imageCollections.ftl").arg("collections",
                getCollections(bank, "image")).arg("collection", collection).arg(
                "bank", bank);
    }

    @GET
    @Path("{bank}/image/{collection}/view")
    public Object getImageCollectionView(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            return getTemplate("imageCollection.ftl").arg("images",
                    BankManager.getItemsInCollection(bank, "image", collection)).arg(
                    "collection", collection).arg("bank", bank);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{bank}/image/{collection}/info")
    public Object getImageCollectionInfo(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        try {
            return BankManager.getInfoFile(bank, "image", collection);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{bank}/image/{collection}/{resource}")
    public Response getImage(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        File file;
        try {
            file = BankManager.getImageFile(bank, collection, resource);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
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
        return getTemplate("image.ftl").arg("path", path).arg("bank", bank).arg(
                "resource", resource).arg("collection", collection);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bank}/json/images")
    public String listImages(@PathParam("bank") String bank) {
        String path = String.format("%s/image/", bank);
        JSONArray index = new JSONArray();
        File file;
        try {
            file = BankManager.getFile(path);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
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
    @Path("json/tree")
    public String getTree() {
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

            JSONArray folderTypes = new JSONArray();
            folderTypes.add(getNavTreeNode(bankName, "style"));
            folderTypes.add(getNavTreeNode(bankName, "preset"));
            folderTypes.add(getNavTreeNode(bankName, "image"));
            folderTypes.add(getNavTreeNode(bankName, "skins"));
            bankNode.put("children", folderTypes);

            tree.add(bankNode);
        }
        return tree.toString();
    }

    private JSONObject getNavTreeNode(String bankName, String typeName) {
        JSONObject folderTypeNode = new JSONObject();
        JSONObject folderTypeMap = new JSONObject();
        folderTypeMap.put("title", typeName);

        JSONObject folderTypeAttributes = new JSONObject();
        String folderTypeName = "folder";
        if ("skins".equals(typeName)) {
            folderTypeName = "skins";
        }
        folderTypeAttributes.put("rel", folderTypeName);
        folderTypeAttributes.put("path",
                String.format("/%s/%s", bankName, typeName));
        folderTypeAttributes.put("id",
                BankUtils.getDomId(String.format("%s-%s", bankName, typeName)));
        folderTypeNode.put("attributes", folderTypeAttributes);
        folderTypeNode.put("data", folderTypeMap);

        if ("skins".equals(typeName)) {
            JSONArray skinItems = new JSONArray();
            try {
                for (String c : BankManager.getCollections(bankName, "style")) {
                    Map<String, Object> info = BankManager.getInfo(bankName,
                            "style", c);
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
                        if (!isSkin) {
                            continue;
                        }
                        JSONObject itemNode = new JSONObject();
                        JSONObject itemMap = new JSONObject();
                        itemMap.put("title", resource);

                        JSONObject itemAttributes = new JSONObject();
                        itemAttributes.put("rel", "skin");
                        itemAttributes.put("path", String.format(
                                "/%s/style/%s/%s", bankName, c, resource));
                        itemAttributes.put("id",
                                BankUtils.getDomId(String.format("%s-%s-%s-%s",
                                        bankName, typeName, c, resource)));
                        itemNode.put("attributes", itemAttributes);
                        itemNode.put("data", itemMap);

                        skinItems.add(itemNode);
                    }
                }
            } catch (IOException e) {
                throw new ThemeBankException(e.getMessage(), e);
            }
            folderTypeNode.put("children", skinItems);
        } else {
            JSONArray collections = new JSONArray();
            for (String c : getCollections(bankName, typeName)) {
                JSONArray items = new JSONArray();

                JSONObject collectionNode = new JSONObject();
                JSONObject collectionMap = new JSONObject();
                collectionMap.put("title", c);

                JSONObject collectionAttributes = new JSONObject();
                collectionAttributes.put("rel", "collection");
                collectionAttributes.put("path",
                        String.format("/%s/%s/%s", bankName, typeName, c));
                collectionAttributes.put("id",
                        BankUtils.getDomId(String.format("%s-%s-%s", bankName,
                                typeName, c)));
                collectionNode.put("attributes", collectionAttributes);
                collectionNode.put("data", collectionMap);

                Boolean isStyle = "style".equals(typeName);
                List<String> skins = new ArrayList<String>();
                if (isStyle) {
                    skins = listSkinsInCollection(bankName, c);
                }

                for (String item : getItemsInCollection(bankName, typeName, c)) {

                    String effectiveTypeName = typeName;
                    if (isStyle && skins.contains(item)) {
                        effectiveTypeName = "skin";
                    }
                    JSONObject itemNode = new JSONObject();
                    JSONObject itemMap = new JSONObject();
                    itemMap.put("title", item);

                    JSONObject itemAttributes = new JSONObject();
                    itemAttributes.put("rel", effectiveTypeName);
                    itemAttributes.put("path", String.format("/%s/%s/%s/%s",
                            bankName, typeName, c, item));
                    itemAttributes.put("id", BankUtils.getDomId(String.format(
                            "%s-%s-%s-%s", bankName, typeName, c, item)));
                    itemNode.put("attributes", itemAttributes);
                    itemNode.put("data", itemMap);

                    items.add(itemNode);
                }
                collectionNode.put("children", items);
                collections.add(collectionNode);
            }
            folderTypeNode.put("children", collections);
        }
        return folderTypeNode;
    }

    /*
     * API
     */
    public static List<String> getBankNames() {
        return BankManager.getBankNames();
    }

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

    public static List<String> getCollections(String bank, String typeName) {
        try {
            return BankManager.getCollections(bank, typeName);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    public static List<String> getItemsInCollection(String bank,
            String typeName, String collection) {
        try {
            return BankManager.getItemsInCollection(bank, typeName, collection);
        } catch (IOException e) {
            throw new ThemeBankException(e.getMessage(), e);
        }
    }

    private Object noPreview() {
        return redirect(ctx.getModulePath() + "/skin/img/no-preview.png");
    }

}
