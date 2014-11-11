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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.StreamBlob;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * Helper class to run remote scripts.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Scripting {

    public static String run(File script, Map<String, String> args)
            throws IOException {
        FileInputStream in = new FileInputStream(script);
        try {
            return run(script.getName(), in, args);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }

    }

    public static String run(String resource, Map<String, String> args)
            throws IOException {
        InputStream in = Scripting.class.getClassLoader().getResourceAsStream(
                resource);
        if (in == null) {
            throw new FileNotFoundException("No such resource: " + resource);
        }
        try {
            return run(resource, in, args);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public static String run(URL url, Map<String, String> args)
            throws IOException {
        InputStream in = url.openStream();
        try {
            return run(url.getFile(), in, args);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public static String run(String name, InputStream in,
            Map<String, String> args) {
        try {
            return runScript(Shell.get().getContextObject(RemoteContext.class),
                    new StreamBlob(in, name, "text/plain"), args);
        } catch (ShellException e) {
            throw e;
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

    public static String runScript(RemoteContext ctx, Blob blob,
            Map<String, String> args) throws Exception {
        String fname = blob.getFileName();
        if (fname != null) {
            if (fname.endsWith(".groovy")) {
                fname = "groovy";
            } else {
                fname = null;
            }
        }
        if (args == null) {
            args = new HashMap<String, String>();
        }
        OperationRequest req = ctx.getSession().newRequest(
                "Context.RunInputScript", args).setInput(blob);
        if (fname != null) {
            req.set("type", fname);
        }
        Blob response = (Blob) req.execute();
        if (response != null) {
            InputStream in = response.getStream();
            String str = null;
            try {
                str = FileSystem.readContent(in);
            } finally {
                in.close();
                if (response instanceof FileBlob) {
                    ((FileBlob) response).getFile().delete();
                }
            }
            return str;
        }
        return null;
    }

}
