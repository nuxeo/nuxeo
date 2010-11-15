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
 *     bstefanescu
 */
package org.nuxeo.ecm.shell.automation.cmds;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.fs.FileCompletor;
import org.nuxeo.ecm.shell.fs.FileSystem;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "audit", help = "Run a query against audit service")
public class Audit implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-ctx", hasValue = true, help = "Use this to set query variables. Syntax is: \"k1=v1,k1=v2\"")
    protected String queryVars;

    @Parameter(name = "-s", hasValue = true, help = "Use this to change the separator used in query variables. THe default is ','")
    protected String sep = ",";

    @Parameter(name = "-f", hasValue = true, completor = FileCompletor.class, help = "Use this to save results in a file. Otherwise results are printed on the screen.")
    protected File file;

    @Parameter(name = "-max", hasValue = true, help = "The max number of rows to return.")
    protected int max;

    @Parameter(name = "-page", hasValue = true, help = "The current page to return. To be used in conjunction with -max.")
    protected int page = 1;

    @Argument(name = "query", index = 0, required = true, help = "The query to run. Query is in JPQL format")
    protected String query;

    public void run() {
        try {
            OperationRequest req = ctx.getSession().newRequest("Audit.Query").set(
                    "query", query).set("maxResults", max).set("pageNo", page);
            if (queryVars != null) {
                for (String pair : queryVars.split(sep)) {
                    String[] ar = StringUtils.split(pair, '=', true);
                    req.setContextProperty("audit.query." + ar[0], ar[1]);
                }
            }
            Blob blob = (Blob) req.execute();
            String content = null;
            if (file != null) {
                ((FileBlob) blob).getFile().renameTo(file);
            } else {
                InputStream in = blob.getStream();
                try {
                    content = FileSystem.readContent(in);
                } finally {
                    in.close();
                    ((FileBlob) blob).getFile().delete();
                }
                print(ctx.getShell().getConsole(), content);
            }
        } catch (Exception e) {
            throw new ShellException("Failed to query audit.", e);
        }
    }

    protected void print(ShellConsole console, String content) {
        JSONArray rows = JSONArray.fromObject(content);
        int len = rows.size();
        SimpleDateFormat fmt = new SimpleDateFormat();
        for (int i = 0; i < len; i++) {
            JSONObject obj = (JSONObject) rows.get(i);
            console.print(obj.optString("eventId"));
            console.print("\t");
            console.print(obj.optString("category"));
            console.print("\t");
            console.print(fmt.format(new Date(obj.optLong("eventDate"))));
            console.print("\t");
            console.print(obj.optString("principal"));
            console.print("\t");
            console.print(obj.optString("docUUID"));
            console.print("\t");
            console.print(obj.optString("docType"));
            console.print("\t");
            console.print(obj.optString("docLifeCycle"));
            console.print("\t");
            console.println(obj.optString("comment"));
        }
    }
}
