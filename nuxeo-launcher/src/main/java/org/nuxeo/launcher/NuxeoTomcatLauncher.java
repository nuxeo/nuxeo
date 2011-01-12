/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.TomcatConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.1
 */
public class NuxeoTomcatLauncher extends NuxeoLauncher {

    // java -Xms512m -Xmx1024m -XX:MaxPermSize=512m
    // -Dsun.rmi.dgc.client.gcInterval=3600000
    // -Dsun.rmi.dgc.server.gcInterval=3600000 -Dfile.encoding=UTF-8
    // -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -classpath
    // :/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat/bin/bootstrap.jar
    // -Dnuxeo.home=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat
    // -Dnuxeo.conf=./bin/nuxeo.conf
    // -Dnuxeo.log.dir=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat/log
    // -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
    // -Dcatalina.base=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat
    // -Dcatalina.home=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat
    // -Djava.io.tmpdir=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat/tmp
    // -Dnuxeo.data.dir=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat/nxserver/data
    // -Dnuxeo.tmp.dir=/Users/julien/Documents/workspace/nuxeo-ecm/nuxeo-distribution/nuxeo-distribution-tomcat/target/nuxeo-dm-5.4.1-SNAPSHOT-tomcat/tmp
    // org.apache.catalina.startup.Bootstrap start

    /**
     * @param configurationGenerator
     */
    public NuxeoTomcatLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        ArrayList<String> serverProperties = new ArrayList<String>();
        serverProperties.add("-Djava.util.logging.manager="
                + "org.apache.juli.ClassLoaderLogManager");
        serverProperties.add("-Dcatalina.base="
                + configurationGenerator.getNuxeoHome().getPath());
        serverProperties.add("-Dcatalina.home="
                + configurationGenerator.getNuxeoHome().getPath());
        return serverProperties;
    }

    @Override
    protected void setServerProperties(Map<String, String> env) {
        env.put("java.util.logging.manager",
                "org.apache.juli.ClassLoaderLogManager");
        env.put("catalina.base",
                configurationGenerator.getNuxeoHome().getPath());
        env.put("catalina.home",
                configurationGenerator.getNuxeoHome().getPath());
    }

    @Override
    protected String getClassPath() {
        // String cp = getJarLauncher().getPath();
        String cp = ".";
        // cp = addToClassPath(cp,"bin" + File.separator +
        // "nuxeo-launcher.jar");
        cp = addToClassPath(cp, "nxserver" + File.separator + "lib");
        cp = addToClassPath(cp, "bin" + File.separator + "bootstrap.jar");
        return cp;
    }

    @Override
    protected void setServerStartCommand(List<String> command) {
        command.add(TomcatConfigurator.STARTUP_CLASS);
        command.add("start");
    }

    @Override
    protected void setServerStopCommand(List<String> command) {
        command.add(TomcatConfigurator.STARTUP_CLASS);
        command.add("stop");
    }

    @Override
    protected String getServerPrint() {
        return TomcatConfigurator.STARTUP_CLASS;
    }
}
