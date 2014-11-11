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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.resource.ResourceException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.deployment.autodeploy.AutoDeployService;
import org.glassfish.embed.AppServer;
import org.glassfish.embed.impl.EmbeddedAPIClassLoaderServiceImpl;
import org.glassfish.embed.impl.EmbeddedDomainXml;
import org.glassfish.embed.impl.EmbeddedServerEnvironment;
import org.glassfish.embed.impl.EmbeddedWebDeployer;
import org.glassfish.embed.impl.EntityResolverImpl;
import org.glassfish.embed.impl.ScatteredWarHandler;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.web.WebEntityResolver;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.Inhabitants;
import org.nuxeo.common.Environment;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.security.SecuritySniffer;
import com.sun.enterprise.v3.admin.adapter.AdminConsoleAdapter;
import com.sun.enterprise.v3.common.PlainTextActionReporter;
import com.sun.enterprise.v3.server.APIClassLoaderServiceImpl;
import com.sun.enterprise.v3.server.DomainXml;
import com.sun.enterprise.v3.server.DomainXmlPersistence;
import com.sun.enterprise.v3.services.impl.LogManagerService;
import com.sun.enterprise.web.WebDeployer;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.hk2.component.InhabitantsParser;
import com.sun.web.security.RealmAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GlassFishServer extends AppServer {

    //private static final Log log = LogFactory.getLog(GlassFishServer.class);

    public GlassFishServer(URL domainXmlUrl) {
        super(domainXmlUrl);
    }

    public GlassFishServer(int port) {
        super(port);
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
                habitat.getComponent(CommandRunner.class);
        commandRunner.doCommand(command, args, reporter);
        return reporter;
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

    public boolean pingJdbcConnectionPool(String poolName) throws ResourceException {
        ConnectorRuntime connRuntime = habitat.getComponent(ConnectorRuntime.class);
        return connRuntime.pingConnectionPool(poolName);
    }

    // remove parser.drop(DecoratorForJ2EEInstanceListener.class); from super
    @Override
    protected InhabitantsParser decorateInhabitantsParser(InhabitantsParser parser) {
        // registering the server using the base class and not the current instance class
        // (GlassFish server may be extended by the user)
        parser.habitat.add(new ExistingSingletonInhabitant<AppServer>(AppServer.class, this));
        // register scattered web handler before normal WarHandler kicks in.
        Inhabitant<ScatteredWarHandler> swh = Inhabitants.create(new ScatteredWarHandler());
        parser.habitat.add(swh);
        parser.habitat.addIndex(swh, ArchiveHandler.class.getName(), null);

        // we don't want GFv3 to reconfigure all the loggers
        parser.drop(LogManagerService.class);

        // we don't need admin CLI support.
        // TODO: admin CLI should be really moved to a separate class
        parser.drop(AdminConsoleAdapter.class);

        // don't care about auto-deploy either
        try {
            Class.forName("org.glassfish.deployment.autodeploy.AutoDeployService");
            parser.drop(AutoDeployService.class);
        }
        catch (Exception e) {
            // ignore.  It may not be available
        }

        //TODO: workaround for a bug
//        parser.replace(ApplicationLifecycle.class, EmbeddedApplicationLifecycle.class);

        parser.replace(APIClassLoaderServiceImpl.class, EmbeddedAPIClassLoaderServiceImpl.class);
        // we don't really parse domain.xml from disk
        parser.replace(DomainXml.class, EmbeddedDomainXml.class);

        // ... and we don't persist it either.
        parser.replace(DomainXmlPersistence.class, EmbeddedDomainXml.class);

        // we provide our own ServerEnvironment
        parser.replace(ServerEnvironmentImpl.class, EmbeddedServerEnvironment.class);

        {// adjustment for webtier only bundle
            //parser.drop(DecoratorForJ2EEInstanceListener.class);

            // in the webtier-only bundle, these components don't exist to begin with.

            try {
                // security code needs a whole lot more work to work in the modular environment.
                // disabling it for now.
                parser.drop(SecuritySniffer.class);

                // WebContainer has a bug in how it looks up Realm, but this should work around that.
                parser.drop(RealmAdapter.class);
            } catch (LinkageError e) {
                // maybe we are running in the webtier only bundle
            }
        }

        // override the location of default-web.xml
        parser.replace(WebDeployer.class, EmbeddedWebDeployer.class);

        // override the location of cached DTDs and schemas
        parser.replace(WebEntityResolver.class, EntityResolverImpl.class);

        return parser;
    }

    /**
     * In fact this returns the instance directory.
     * See {@link org.glassfish.server.ServerEnvironmentImpl} and
     * {@link com.sun.enterprise.module.bootstrap.StartupContext}.
     * Should override this otherwise jdbc.ra dirs will not be correctly set
     */
    @Override
    protected File createTempDir() throws IOException {
        // must simulate as if we are in glassfish/bin dir so that ${home}/glassfish is selected as the glassfish root
        return new File(Environment.getDefault().getHome().getAbsolutePath() + "/glassfish/bin");
    }

}
