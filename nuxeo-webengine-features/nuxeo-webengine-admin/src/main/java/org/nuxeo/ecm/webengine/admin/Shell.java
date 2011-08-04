/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.ecm.webengine.admin;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("/shell")
@WebObject(type = "Shell")
public class Shell extends ModuleRoot {
    private static final Log log = LogFactory.getLog(Shell.class);

    @GET
    @Produces("text/html;charset=UTF-8")
    public Object getShell() {
        return getView("shell");
    }

    @GET
    @Path("shell.jnlp")
    @Produces("application/x-java-jnlp-file")
    public Object getShellJnlp() {
        return getView("shell.jnlp");
    }

    @GET
    @Path("applet.jnlp")
    @Produces("application/x-java-jnlp-file")
    public Object getAppletJnlp() {
        return getView("applet.jnlp");
    }

    @GET
    @Path("shell.jar")
    @Produces("application/java-archive")
    public Object getShellJar() throws URISyntaxException {
        File file = null;
        try {
            URL url = Class.forName("org.nuxeo.shell.Shell").getProtectionDomain().getCodeSource().getLocation();
            return new File(url.toURI());
        } catch (ClassNotFoundException e) {
            log.debug(e);
            file = new File(Environment.getDefault().getServerHome(), "client");
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    String name = f.getName();
                    if (name.endsWith(".jar") && name.contains("shell")) {
                        return f;
                    }
                }
            }
        }
        return redirect("http://www.nuxeo.org/static/latest-release/nuxeo-shell");
    }

}
