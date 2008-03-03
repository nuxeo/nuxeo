/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// name is required by config
@XObject(value = "repository", order = { "@name" })
public class RepositoryDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@factory")
    private Class factoryClass;

    private String home;
    private String config;

    @XNode("@securityManager")
    private Class securityManager;

    @XNode("@forceReloadTypes")
    private boolean forceReloadTypes = false;

    private final File REPOS_DIR =
            new File(Framework.getRuntime().getHome(), "repos");


    public RepositoryDescriptor() {
    }

    public RepositoryDescriptor(String name, Class factoryClass, String home, String config,
            boolean forceReloadTypes) {
        this.name = name;
        this.factoryClass = factoryClass;
        this.home = home;
        this.config = config;
        this.forceReloadTypes = forceReloadTypes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getHomeDirectory() {
        if (home == null || home.length() == 0) {
            File homeFile = new File(REPOS_DIR, name);
            home = homeFile.getAbsolutePath();
            homeFile.mkdirs();
        }
        return home;
    }

    /**
     * @param home the home directory to set
     */
    public void setHomeDirectory(String home) {
        this.home = home;
    }

    public String getConfigurationFile() {
        return config;
    }

    @XContent
    public void setConfigurationContent(String content) {
        String homePath = getHomeDirectory();
        File configFile = new File(homePath, name + ".xml");
        config = configFile.getAbsolutePath();
        try {
            FileUtils.writeFile(configFile, content);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
    }

    /**
     * @param config the config to set
     */
    public void setConfigurationFile(String config) {
        this.config = config;
    }

    /**
     * @param factoryClass the factoryClass to set
     */
    public void setFactoryClass(Class factoryClass) {
        this.factoryClass = factoryClass;
    }

    /**
     * @return the factoryClass
     */
    public Class getFactoryClass() {
        return factoryClass;
    }

    public void setSecurityManagerClass(Class securityManager) {
        this.securityManager = securityManager;
    }

    public Class getSecurityManagerClass() {
        return securityManager;
    }

    public SecurityManager getSecurityManager() throws IllegalAccessException,
            InstantiationException {
        if (securityManager == null) {
            return null;
        }
        return (SecurityManager) securityManager.newInstance();
    }

    public final Repository create() throws Exception {
        return getFactory().createRepository(this);
    }

    public final RepositoryFactory getFactory() throws IllegalAccessException,
            InstantiationException {
        assert factoryClass != null;
        return (RepositoryFactory) factoryClass.newInstance();
    }

    public boolean getForceReloadTypes() {
        return forceReloadTypes;
    }

    public void setForceReloadTypes(boolean val) {
        forceReloadTypes = val;
    }

    public void dispose() {
        factoryClass = null;
        securityManager = null;
        name = null;
        home = null;
        config = null;
    }

    @Override
    public String toString() {
        return "Repository " + name + " { home: " + home + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RepositoryDescriptor) {
            RepositoryDescriptor rd = (RepositoryDescriptor) obj;
            return name.equals(rd.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
