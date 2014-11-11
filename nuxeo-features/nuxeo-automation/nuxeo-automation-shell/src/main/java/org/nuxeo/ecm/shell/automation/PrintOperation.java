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
package org.nuxeo.ecm.shell.automation;

import java.io.File;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpConnector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultSession;
import org.nuxeo.ecm.automation.client.jaxrs.util.Base64;
import org.nuxeo.ecm.automation.client.jaxrs.util.IOUtils;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Command(name = "print", help = "Print operation(s) definition")
public class PrintOperation implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-u", hasValue = true, help = "The username if any.")
    protected String u;

    @Parameter(name = "-p", hasValue = true, help = "The password if any.")
    protected String p;

    @Parameter(name = "-out", hasValue = true, help = "An optional file to save the operation definition into. If not used the definition will be printed on stdout.")
    protected File out;

    @Argument(name = "operation", index = 0, required = false, completor = OperationNameCompletor.class, help = "The opertation to print.")
    protected String name;

    public void run() {
        try {
            String url = ctx.getClient().getBaseUrl();
            HttpGet get = new HttpGet(url + (name == null ? "" : name));
            if (u != null && p != null) {
                //TOOD be able to reuse the context of the automation client
                get.setHeader("Authorization", "Basic "+Base64.encode(u+":"+p));
            }
            HttpResponse r = ctx.getClient().http().execute(get);
            InputStream in = r.getEntity().getContent();
            String content = IOUtils.read(in);
            if (out == null) {
                ctx.getShell().getConsole().println(content);
            } else {
                IOUtils.writeToFile(content, out);
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

}
