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
 */
package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.osgi.application.FrameworkBootstrap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Deployer {

    /**
     * Run the preprocessing
     * @param args
     */
    public static void main(String[] args) throws Exception {
        File basedir = null;
        File home = null;
        File war = null;
        boolean updateWar = false;
        if (System.getProperty("updateWar") != null) {
            updateWar = true;
        }
        //System.out.println(">>>" +args.length);
        if (args.length == 0) {
            basedir = new File(".").getCanonicalFile().getParentFile();
            war = new File(basedir, "webapps/nuxeo");
            home = new File(war, "nxserver");
            //System.out.println(">>>>>>>>>>>"+basedir+", "+war);
        } else if (args.length == 1) {
            basedir = new File(args[0]);            
            war = new File(basedir, "webapps/nuxeo");
            home = new File(war, "nxserver");            
        } else if (args.length == 2) {
            war = new File(basedir, "webapps/nuxeo");
            home = new File(war, "nxserver");                        
        } else {
            System.err.println("Usage: deploy.sh [tomcatBaseDir] [nuxeoWar] or just 'deploy.sh' from tomcat bin directoy");
        }
        basedir = basedir.getCanonicalFile();
        home = home.getCanonicalFile();
        war = war.getCanonicalFile();
        System.out.println("Using tomcat directory directory: "+basedir);
        System.out.println("Using nuxeo war: "+war);
        System.out.println("Using nuxeo instance: "+home);
                
        // build classpath
        List<URL> urls = new ArrayList<URL>();
        File libs = new File(basedir, "lib");
        File[] files = libs.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }
        
        libs = new File(home, "lib");
        files = libs.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }

        libs = new File(home, "bundles");
        files = libs.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
        }


        URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), Deployer.class.getClassLoader());

        System.out.println("# Running preprocessor ...");
        Class<?> klass = cl.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
        Method main = klass.getMethod("main", String[].class);
        main.invoke(null, new Object[] {new String[] {home.getAbsolutePath()}});
        System.out.println("# Preprocessing done.");

        if (!updateWar) {
            System.out.println("Not updating WAR.");
            return;
        }
        
        System.out.println("Updating WAR");
        // Now we should move nuxeo.war content to nuxeo ... but make sure you do not override 
        // jsf-impl jar and META-INF/context.xml
        File nuxeoWar = new File(home, "nuxeo.war");        
        File tmp = new File(basedir, "temp/nuxeo");
        nuxeoWar.renameTo(tmp);
        // copy over context.xml and jsf-lib
        File ctx = new File(war, "META-INF/context.xml");
        FrameworkBootstrap.copyFile(ctx, new File(tmp, "META-INF/context.xml"));
        File jsf = FrameworkBootstrap.findFileStartingWidth(new File(war, "WEB-INF/lib"), "jsf-impl-");
        if (jsf != null) {
            FrameworkBootstrap.copyFile(ctx, new File(new File(tmp, "WEB-INF/lib"), jsf.getName()));
        } else {
            System.out.println("WARN: no jsf-impl file found");
        }
        // move nxserver into new war.
        //FrameworkBootstrap.copyTree(home, tmp);
        home.renameTo(new File(tmp, home.getName()));
        // replace the old war. Do a copy if rename fails
        if (!tmp.renameTo(war)) {
            System.out.println("rename failed try to delete and copy");
            FrameworkBootstrap.deleteAll(war);
            if (!tmp.renameTo(war)) {
                System.out.println("rename failed again");
                FrameworkBootstrap.copyTree(tmp, war.getParentFile());
                FrameworkBootstrap.deleteAll(tmp);
            }
        }
        System.out.println("Done.");
    }


}
