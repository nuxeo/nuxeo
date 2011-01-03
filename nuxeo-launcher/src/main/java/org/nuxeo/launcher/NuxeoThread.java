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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.commons.MutableClassLoader;
import org.nuxeo.launcher.commons.MutableClassLoaderDelegate;
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.Environment;

/**
 * @author jcarsique
 *
 */
public abstract class NuxeoThread extends Thread {
    static final Log log = LogFactory.getLog(NuxeoThread.class);

    protected ConfigurationGenerator configurationGenerator;

    @SuppressWarnings("rawtypes")
    protected Class startupClass;

    public NuxeoThread(ConfigurationGenerator configurationGenerator) {
        super("Nuxeo");
        this.configurationGenerator = configurationGenerator;
    }

    @Override
    public void run() {
        MutableClassLoader loader = new MutableClassLoaderDelegate(
                getClass().getClassLoader());
        // setJavaProperties();
        setClassPath(loader);
        setSystemProperties();
        setNuxeoSystemProperties();
        log.debug("1");
        configurationGenerator.initLogs();
        log.debug("2");
        System.out.println("2");
        try {
            startServer(loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find startup class", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not instanciate startup class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void setSystemProperties();

    protected abstract void setClassPath(MutableClassLoader loader);

    protected abstract void startServer(MutableClassLoader loader)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException;

    protected void setNuxeoSystemProperties() {
        System.setProperty("nuxeo.home",
                configurationGenerator.getNuxeoHome().getPath());
        System.setProperty("nuxeo.conf",
                configurationGenerator.getNuxeoConf().getPath());
        setNuxeoSystemProperty(Environment.NUXEO_LOG_DIR);
        setNuxeoSystemProperty(Environment.NUXEO_DATA_DIR);
        // setNuxeoSystemProperty(Environment.NUXEO_TMP_DIR);
    }

    private void setNuxeoSystemProperty(String property) {
        log.debug("Set system " + property + ": "
                + configurationGenerator.getUserConfig().getProperty(property));
        System.setProperty(property,
                configurationGenerator.getUserConfig().getProperty(property));
    }

    protected void addToClassPath(MutableClassLoader loader, String filename)
            throws MalformedURLException {
                File classPathEntry = new File(configurationGenerator.getNuxeoHome(),
                        filename);
                if (!classPathEntry.exists()) {
                    throw new RuntimeException(
                            "Tried to add inexistant classpath entry: "
                                    + classPathEntry);
                }
                loader.addURL(classPathEntry.toURI().toURL());
            }

}