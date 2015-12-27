/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
