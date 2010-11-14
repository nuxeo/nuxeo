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
package org.nuxeo.ecm.automation.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    public static String run(File script) throws IOException {
        FileInputStream in = new FileInputStream(script);
        try {
            return run(in);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }

    }

    public static String run(String resource) throws IOException {
        InputStream in = Scripting.class.getClassLoader().getResourceAsStream(
                resource);
        if (in == null) {
            throw new FileNotFoundException("No such resource: " + resource);
        }
        try {
            return run(in);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public static String run(URL url) throws IOException {
        InputStream in = url.openStream();
        try {
            return run(in);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    public static String run(InputStream in) {
        try {
            return runScript(Shell.get().getContextObject(RemoteContext.class),
                    new StreamBlob(in, "script", "text/plain"),
                    new HashMap<String, String>());
        } catch (ShellException e) {
            throw e;
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

    public static String runScript(RemoteContext ctx, Blob blob,
            Map<String, String> args) throws Exception {
        Blob response = (Blob) ctx.getSession().newRequest(
                "Context.RunInputScript", args).setInput(blob).execute();
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
