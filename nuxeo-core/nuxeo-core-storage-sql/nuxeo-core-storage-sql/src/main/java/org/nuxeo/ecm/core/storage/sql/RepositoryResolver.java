/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.List;

import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextParser;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerService;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * @deprecated since 5.9.5, use {@link SQLRepositoryService} or
 *             {@link BinaryManagerService} directly instead
 */
@Deprecated
public class RepositoryResolver {

    private RepositoryResolver() {
    }

    /**
     * @deprecated since 5.9.5, use SQLRepositoryService instead
     */
    @Deprecated
    public static void registerTestRepository(RepositoryImpl repository) {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.registerTestRepository(repository);
    }

    /**
     * Gets the repositories as a list of {@link RepositoryManagement} objects.
     *
     * @return a list of {@link RepositoryManagement}
     * @deprecated since 5.9.5, use
     *             {@link SQLRepositoryService#getRepositoriesManagement}
     *             instead
     */
    @Deprecated
    public static List<RepositoryManagement> getRepositories() {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getRepositories();
    }

    /**
     * Gets a repository as a {@link RepositoryManagement} object.
     *
     * @return the repository
     * @deprecated since 5.9.5, use {@link SQLRepositoryService} instead
     */
    @Deprecated
    public static RepositoryManagement getRepository(String repositoryName) {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getRepository(repositoryName);
    }

    /**
     * @deprecated since 5.9.4, use BinaryManagerService instead
     */
    @Deprecated
    public static BinaryManager getBinaryManager(String repositoryName) {
        BinaryManagerService bms = Framework.getService(BinaryManagerService.class);
        return bms.getBinaryManager(repositoryName);
    }

    /**
     * @deprecated since 5.9.5, use SQLRepositoryService instead
     */
    @Deprecated
    public static Class<? extends FulltextParser> getFulltextParserClass(
            String repositoryName) {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getFulltextParserClass(repositoryName);
    }

    /**
     * @deprecated since 5.9.5, use SQLRepositoryService instead
     */
    @Deprecated
    public static FulltextConfiguration getModelFulltext(String repositoryName) {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getFulltextConfiguration(repositoryName);
    }

}
