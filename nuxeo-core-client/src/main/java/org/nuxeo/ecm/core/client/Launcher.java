/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.client;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.nuxeo.osgi.application.client.NuxeoApp;

/**
 * @author <a href="mailto:gbamberger@nuxeo.com">Gaelle Bamberger</a>
 */
public class Launcher implements Runnable {

    public void addProperties(Hashtable<String, String> props){
        for (Entry<String, String> entry : props.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    protected static File getLibDir() throws Exception {
        String path = "";
        File file = null;
        URL url = Launcher.class.getClassLoader().getResource("target/classes/lib/osgi-core-4.1.jar");
        if (url != null){
            path = url.getPath();
            file = new File(path).getParentFile();
        }
        else {
            url = Launcher.class.getClassLoader().getResource("lib/osgi-core-4.1.jar");
            path = url.getPath();
            file = new File(path).getParentFile();
        }
        return file;
    }

    public static Collection<File> getBundles() throws Exception {
        List<File> bundles = new ArrayList<File>();
        File libDir = getLibDir();
        for (String name : libDir.list()) {
            if (name.startsWith("nuxeo-") && !name.contains("osgi") && !name.contains(".jar-")) {
                bundles.add(new File(libDir, name));
            }
        }
        return bundles;
    }

    protected static File setProperties() throws IOException{
      //Set system properties
        //we don't need the streaming server as we are client
        System.setProperty("org.nuxeo.runtime.streaming.isServer", "false");
        //client only
        System.setProperty("org.nuxeo.runtime.server.enabled", "false");
        //Create a folder for the running of the application
        File home = File.createTempFile("nuxeo-", ".tmp");
        home.delete();
        home.mkdirs();
        home.deleteOnExit();
        return home;
    }

    /**
     * Creates a nuxeo application.
     */
    protected static NuxeoApp setup() throws Exception {
        File home = Launcher.setProperties();
        System.setProperty("nuxeo.runner", Launcher.class.getName());
        return new NuxeoApp(home, Launcher.class.getClassLoader());
    }

    /**
     * Launch application.
     */
    public static void main(String[] args) throws Exception {
        NuxeoApp app = setup();
        Collection<File> bundles = getBundles();
        app.start();

        //Deploy bundles if it doesn't exist
        if (bundles != null) {
            app.deployBundles(bundles);
        }

        //Load application
        ClassLoader cl = app.getLoader();
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            String main = System.getProperty("nuxeo.runner");
            if (main == null) {
                throw new Error("No runnable specified");
            }
            Class<?> mc = app.getLoader().loadClass(main);
            Method m = mc.getMethod("run");
            m.invoke(mc.newInstance());
        } finally {
            Thread.currentThread().setContextClassLoader(oldcl);
            app.shutdown();
        }
    }

    public void run(){
    }
}
