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

import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.embed.GlassFish;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitants;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.v3.admin.CommandRunner;
import com.sun.enterprise.v3.common.PlainTextActionReporter;

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
