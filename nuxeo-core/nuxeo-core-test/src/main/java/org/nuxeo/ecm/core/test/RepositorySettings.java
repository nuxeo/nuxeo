/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Repository configuration that can be set using {@link RepositoryConfig} annotations.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RepositorySettings {

    protected FeaturesRunner runner;

    protected RepositoryInit repositoryInit;

    protected Granularity granularity;

    protected Class<? extends RepositoryFactory> repositoryFactoryClass;

    public RepositorySettings(FeaturesRunner runner) {
        this.runner = runner;
        RepositoryConfig conf = runner.getConfig(RepositoryConfig.class);
        if (conf == null) {
            conf = Defaults.of(RepositoryConfig.class);
        }
        importAnnotations(conf);
    }

    public void importAnnotations(RepositoryConfig repo) {
        Granularity cleanup = repo.cleanup();
        granularity = cleanup == Granularity.UNDEFINED ? Granularity.CLASS : cleanup;
        repositoryFactoryClass = repo.repositoryFactoryClass();
        repositoryInit = newInstance(repo.init());
    }

    protected <T> T newInstance(Class<? extends T> clazz) {
        try {
            return clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public RepositoryInit getRepositoryInit() {
        return repositoryInit;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public Class<? extends RepositoryFactory> getRepositoryFactoryClass() {
        return repositoryFactoryClass;
    }

}
