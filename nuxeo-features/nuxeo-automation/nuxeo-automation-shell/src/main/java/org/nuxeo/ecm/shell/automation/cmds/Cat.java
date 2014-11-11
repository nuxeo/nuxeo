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

import java.text.SimpleDateFormat;
import java.util.TreeSet;

import jline.ANSIBuffer;

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertiesHelper;
import org.nuxeo.ecm.automation.client.jaxrs.model.PropertyMap;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "cat", help = "Print document details")
public class Cat implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-schemas", hasValue = true, help = "A filter of schemas to include in the document. Use * for all schemas.")
    protected String schemas;

    @Parameter(name = "-all", hasValue = false, help = "Include all schemas. The -schemas attribute will be ignored if used in conjunction with this one.")
    protected boolean all;

    @Argument(name = "doc", index = 0, required = false, completor = DocRefCompletor.class, help = "The document to print. To use UIDs as refences you should prefix them with 'doc:'")
    protected String path;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();
        if (all) {
            schemas = "*";
        }
        Document doc = ctx.resolveDocument(path, schemas);
        print(console, doc);
    }

    public static void print(ShellConsole console, Document doc) {
        ANSIBuffer buf = new ANSIBuffer();
        buf.append(ShellConsole.CRLF);
        buf.bold(doc.getType()).append(" -- ").append(doc.getTitle());
        buf.append(ShellConsole.CRLF);
        buf.append("\tUID: ").append(doc.getId());
        buf.append(ShellConsole.CRLF);
        buf.append("\tPath: ").append(doc.getPath());
        buf.append(ShellConsole.CRLF);
        if (doc.getLastModified() != null) {
            buf.append("\tLast Modified: ").append(
                    new SimpleDateFormat().format(doc.getLastModified()));
            buf.append(ShellConsole.CRLF);
        }
        buf.append("\tState: ").append(
                doc.getState() == null ? "none" : doc.getState());
        buf.append(ShellConsole.CRLF);
        buf.append("\tLock: ").append(
                doc.getLock() == null ? "none" : doc.getLock());
        buf.append(ShellConsole.CRLF);
        buf.append(ShellConsole.CRLF);

        String desc = doc.getString("dc:description");
        if (desc != null && desc.length() > 0) {
            buf.bold("DESCRIPTION");
            buf.append(ShellConsole.CRLF);
            for (String line : StringUtils.split(desc, '\n', true)) {
                buf.append("\t").append(line).append(ShellConsole.CRLF);
            }
            buf.append(ShellConsole.CRLF);
        }
        PropertyMap props = doc.getProperties();
        if (props != null && !props.isEmpty()) {
            TreeSet<String> keys = new TreeSet<String>(props.getKeys());
            buf.bold("PROPERTIES");
            buf.append(ShellConsole.CRLF);
            for (String key : keys) {
                if ("dc:description".equals(key)) {
                    continue;
                }
                Object obj = props.get(key);
                buf.append("\t").append(key).append(" = ");
                if (obj != null) {
                    if (PropertiesHelper.isScalar(obj)) {
                        buf.append(props.getString(key));
                    } else {
                        buf.append(obj.toString());
                    }
                }
                buf.append(ShellConsole.CRLF);
            }
            buf.append(ShellConsole.CRLF);
        }

        console.println(buf.toString());

    }
}
