/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;

/**
 * Repository descriptor wrapping a more specific low-level (VCS) repository
 * descriptor.
 */
// name is required by config
@XObject(value = "repository", order = { "@name" })
public class RepositoryDescriptor {

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    @XNode("@name")
    private String name;

    @XNode("@factory")
    private Class<RepositoryFactory> factoryClass;

    /**
     * Note, this xpath corresponds to an element two levels deep, that will
     * therefore appear in the "repository" element of the underlying repository
     * implementation (VCS) descriptor
     * (org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor). This is done
     * because all the content of the "repository" element of this (core)
     * descriptor is passed as an XML file parsed by the VCS descriptor, so we
     * cannot have the "pool" element as a normal subelement of this core
     * descriptor.
     */
    @XNode("repository/pool")
    public NuxeoConnectionManagerConfiguration pool;

    private String home;

    private String config;

    private final File REPOS_DIR =
            new File(Framework.getRuntime().getHome(), "repos");

    public RepositoryDescriptor() {
    }

    /** Copy constructor. */
    public RepositoryDescriptor(RepositoryDescriptor other) {
        name = other.name;
        factoryClass = other.factoryClass;
        pool = other.pool == null ? null
                : new NuxeoConnectionManagerConfiguration(other.pool);
        home = other.home;
        config = other.config;
    }

    public void merge(RepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.factoryClass != null) {
            factoryClass = other.factoryClass;
        }
        if (other.pool != null) {
            pool = new NuxeoConnectionManagerConfiguration(other.pool);
        }
        if (other.home != null) {
            home = other.home;
        }
        if (other.config != null) {
            config = other.config;
        }
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

    public NuxeoConnectionManagerConfiguration getPool() {
        return pool;
    }

    public final Repository create() {
        return getFactory().createRepository(this);
    }

    public final RepositoryFactory getFactory() {
        if (factoryClass == null) {
            throw new RuntimeException("Bad factory for repository: " + name);
        }
        try {
            return factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Cannot instantiate repository" + name,
                    e);
        }
    }

    public void dispose() {
        factoryClass = null;
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
