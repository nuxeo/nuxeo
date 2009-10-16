/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.osgi.application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestMain {

    public static final String CONFIG_FILE = ".properties";
    public static final String BUNDLES = "bundles";
    public static final String INSTALL_DIR = "installdir";
    public static final String LIB_DIR = "libdir";
    public static final Pattern STR_LIST = Pattern.compile("\\s,\\s");

    public static void main(String[] args) {
        try {
            CommandLineOptions cmdArgs = new CommandLineOptions(args);

            //String clear = cmdArgs.getOption("c");

            File configFile;
            String cfg = cmdArgs.getOption("f");
            if (cfg != null) {
                configFile = new File(cfg);
            } else {
                configFile = new File(CONFIG_FILE);
            }

            String installDirProp;
            String bundlesList = null;
            String libList;
            if (configFile.isFile()) {
                Properties config = new Properties();
                InputStream in = new BufferedInputStream(new FileInputStream(
                        configFile));
                config.load(in);
                installDirProp = config.getProperty(INSTALL_DIR);
                bundlesList = config.getProperty(BUNDLES);
                libList = config.getProperty(LIB_DIR);
            } else {
                installDirProp = cmdArgs.getOption("d");
                bundlesList = cmdArgs.getOption("b");
                libList = cmdArgs.getOption("cp");
            }

            File installDir = null;
            if (installDirProp == null) {
                installDir = new File("."); // current dir
            } else {
                installDir = new File(installDirProp);
            }

            SharedClassLoader cl = new SharedClassLoaderImpl(TestMain.class.getClassLoader());
            if (libList != null) {
                String[] libs = STR_LIST.split(libList, 0);
                //loadLibs(cl, installDir, libs);
            }

        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
