/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.gf3;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.ParameterNames;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embed.GFApplication;
import org.glassfish.embed.GlassFish;
import org.glassfish.embed.ScatteredWar;
import org.glassfish.embed.impl.SilentActionReport;
import org.glassfish.internal.data.ApplicationInfo;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitants;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.v3.admin.CommandRunner;
import com.sun.enterprise.v3.common.PlainTextActionReporter;
import com.sun.enterprise.v3.deployment.DeploymentContextImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GlassFishServer extends GlassFish {

    final static Logger log = Logger.getLogger("GlassFishServer");


    public GlassFishServer(URL domainXmlUrl) {
        super (domainXmlUrl);
    }

    public GlassFishServer(int port) {
        super (port);
    }

    public Habitat getHabitat() {
        return habitat;
    }

    public void loadInhabitant(Object instance) {
        habitat.add(Inhabitants.create(instance));
    }

    public ActionReport execute(String command, Properties args) {
        PlainTextActionReporter reporter = new PlainTextActionReporter();
        CommandRunner commandRunner =
            (CommandRunner) habitat.getComponent(CommandRunner.class);
        return commandRunner.doCommand(command, args, reporter);
    }

    @Override
    public GFApplication deploy(ReadableArchive a) throws IOException {
        Properties params = new Properties();
        params.put(ParameterNames.NAME,a.getName());
        params.put(ParameterNames.ENABLED,"true");
        return deploy(a, params);
    }

    public GFApplication deployWar(ScatteredWar war, String virtualServers, String ctxRoot) throws IOException {
        Properties params = new Properties();
        if (virtualServers == null) {
            virtualServers = "server";
        }
        params.put(ParameterNames.VIRTUAL_SERVERS, virtualServers);
        if (ctxRoot != null) {
            params.put(ParameterNames.CONTEXT_ROOT, ctxRoot);
        }
        return deploy(war, params);
    }

    public GFApplication deployWar(ScatteredWar war, String ctxRoot) throws IOException {
        return deployWar(war, null, ctxRoot);
    }

    public GFApplication deployWar(ScatteredWar war) throws IOException {
        return deployWar(war, null, null);
    }

    /**
     * Must be put into GlassFish - GFApplication ctor was modified to be visible from here
     * @param a
     * @param params
     * @return
     * @throws IOException
     */
    public GFApplication deploy(ReadableArchive a, Properties params) throws IOException {
        ArchiveHandler h = appLife.getArchiveHandler(a);

        // now prepare sniffers
        ClassLoader parentCL = snifMan.createSnifferParentCL(null);
        ClassLoader cl = h.getClassLoader(parentCL, a);
        Collection<Sniffer> activeSniffers = snifMan.getSniffers(a, cl);

        // TODO: we need to stop this totally type-unsafe way of passing parameters
        if (params == null) {
            params = new Properties();
        }
        params.put(ParameterNames.NAME,a.getName());
        params.put(ParameterNames.ENABLED,"true");
        //TODO: custom parameters must be passed as arguments to this method
        params.put(ParameterNames.VIRTUAL_SERVERS, "server");
        params.put(ParameterNames.CONTEXT_ROOT, "/");
        final DeploymentContextImpl deploymentContext = new DeploymentContextImpl(Logger.getAnonymousLogger(), a, params, env);
        deploymentContext.setClassLoader(cl);

        SilentActionReport r = new SilentActionReport();
        ApplicationInfo appInfo = appLife.deploy(activeSniffers, deploymentContext, r);
        r.check();

        return new GFApplication(this,appInfo,deploymentContext);
    }

    public String getVersion() {
        ActionReport report = execute("version", new Properties());
        return report.getMessage();
    }

    public ActionReport createJdbcResource(Properties args) {
        return execute("create-jdbc-resource", args);
    }

    public ActionReport deleteJdbcResource(Properties args) {
        return execute("delete-jdbc-resource", args);
    }

    public ActionReport listJdbcResources(Properties args) {
        return execute("list-jdbc-resources", args);
    }

    public ActionReport createJdbcConnectionPool(Properties args) {
        return execute("create-jdbc-connection-pool", args);
    }

    public ActionReport deleteJdbcConnectionPool(Properties args) {
        return execute("delete-jdbc-connection-pool", args);
    }


    public String[] listJdbcConnectionPools() {
        ActionReport ar = execute("list-jdbc-connection-pools", new Properties());
        if (ar.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        for (MessagePart parts : ar.getTopMessagePart().getChildren()) {
            result.add(parts.getMessage());
        }
        return result.toArray(new String[result.size()]);

    }

    public boolean pingJdbcConnectionPool(String poolName) throws Exception {
        ConnectorRuntime connRuntime = habitat.getComponent(ConnectorRuntime.class);
        return connRuntime.pingConnectionPool(poolName);
    }

}
