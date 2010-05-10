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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import net.sf.json.JSONArray;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.presets.PaletteParser;

@WebObject(type = "theme-banks")
@Produces(MediaType.TEXT_HTML)
public class Main extends ModuleRoot {

    private static final File BANKS_DIR;

    private static final String SERVER_ID = "Nuxeo/ThemeBank-1.0";

    static {
        BANKS_DIR = new File(Framework.getRuntime().getHome(), "theme-banks");
        BANKS_DIR.mkdirs();
    }

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
        return getTemplate("bank.ftl").arg("bank", bank).arg(
                "styleCollections", getCollections(bank, "style")).arg(
                "imageCollections", getCollections(bank, "image"));
    }

    public List<String> getBankNames() {
        List<String> names = new ArrayList<String>();
        for (String bankName : BANKS_DIR.list()) {
            names.add(bankName);
        }
        return names;
    }

    /*
     * Styles
     */
    @GET
    @Path("{bank}/style/{collection}")
    public Object displayStylesInCollection(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("styleCollection.ftl").arg("styles",
                getItemsInCollection(bank, "style", collection)).arg(
                "collection", collection).arg("bank", bank);
    }

    @GET
    @Produces("text/css")
    @Path("{bank}/style/{collection}/{resource}")
    public Response getStyle(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {

        String path = String.format("%s/style/%s/%s", bank, collection,
                resource);
        File file = new File(BANKS_DIR, path);
        if (!file.exists()) {
            return Response.status(404).build();
        }
        return Response.ok().entity(streamFile(file)).lastModified(
                new Date(file.lastModified())).header("Cache-Control", "public").header(
                "Server", SERVER_ID).build();
    }

    /*
     * Presets
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{bank}/preset/{collection}/{category}")
    public Response getPreset(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("category") String category) {
        String path = String.format("%s/preset/%s/%s", bank, collection,
                category);
        File file = new File(BANKS_DIR, path);
        String content = "";
        try {
            StringBuilder sb = new StringBuilder();
            for (File f : file.listFiles()) {
                content = FileUtils.readFile(f);
                content = PaletteParser.renderPaletteAsCsv(content.getBytes(),
                        f.getName());
                sb.append(content);
            }
            content = sb.toString();
        } catch (IOException e) {
            return Response.status(404).build();
        }
        return Response.ok(content).lastModified(new Date(file.lastModified())).header(
                "Cache-Control", "public").header("Server", SERVER_ID).build();
    }

    /*
     * Images
     */
    @GET
    @Path("{bank}/image/{collection}")
    public Object displayImagesInCollection(@PathParam("bank") String bank,
            @PathParam("collection") String collection) {
        return getTemplate("imageCollection.ftl").arg("images",
                getItemsInCollection(bank, "image", collection)).arg(
                "collection", collection).arg("bank", bank);
    }

    @GET
    @Path("{bank}/image/{collection}/{resource}")
    public Response getImage(@PathParam("bank") String bank,
            @PathParam("collection") String collection,
            @PathParam("resource") String resource) {
        String path = String.format("%s/image/%s/%s", bank, collection,
                resource);
        File file = new File(BANKS_DIR, path);
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{bank}/images")
    public String listImages(@PathParam("bank") String bank) {
        String path = String.format("%s/image/", bank);
        JSONArray index=new JSONArray();
        File file = new File(BANKS_DIR, path);
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

    /*
     * Collections
     */
    public List<String> getCollections(String bank, String typeName) {
        List<String> names = new ArrayList<String>();
        File file = new File(BANKS_DIR, String.format("%s/%s", bank, typeName));
        for (String collectionName : file.list()) {
            names.add(collectionName);
        }
        return names;
    }

    public List<String> getItemsInCollection(String bank, String typeName,
            String collection) {
        List<String> names = new ArrayList<String>();
        File file = new File(BANKS_DIR, String.format("%s/%s/%s", bank,
                typeName, collection));
        for (String item : file.list()) {
            names.add(item);
        }
        return names;
    }

    /*
     * Helpers
     */
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
