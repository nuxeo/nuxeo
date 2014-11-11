/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    @XNode("@name")
    private String name;

    @XNode("@factory")
    private Class<RepositoryFactory> factoryClass;

    private String home;
    private String config;

    @XNode("@securityManager")
    private Class<SecurityManager> securityManager;

    @XNode("@forceReloadTypes")
    private boolean forceReloadTypes = false;

    private final File REPOS_DIR =
            new File(Framework.getRuntime().getHome(), "repos");


    public RepositoryDescriptor() {
    }

    public RepositoryDescriptor(String name,
            Class<RepositoryFactory> factoryClass, String home, String config,
            boolean forceReloadTypes) {
        this.name = name;
        this.factoryClass = factoryClass;
        this.home = home;
        this.config = config;
        this.forceReloadTypes = forceReloadTypes;
    }

    public String getName() {
        return name;
    }

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
            log.error(e, e);
        }
    }

    public void setConfigurationFile(String config) {
        this.config = config;
    }

    public void setFactoryClass(Class<RepositoryFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public Class<RepositoryFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setSecurityManagerClass(Class<SecurityManager> securityManager) {
        this.securityManager = securityManager;
    }

    public Class<SecurityManager> getSecurityManagerClass() {
        return securityManager;
    }

    public SecurityManager getSecurityManager() throws IllegalAccessException,
            InstantiationException {
        if (securityManager == null) {
            return null;
        }
        return securityManager.newInstance();
    }

    public final Repository create() throws Exception {
        return getFactory().createRepository(this);
    }

    public final RepositoryFactory getFactory() throws IllegalAccessException,
            InstantiationException {
        assert factoryClass != null;
        return factoryClass.newInstance();
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
