/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ContributionPersistenceManager {

    /**
     * Gets a list with all persisted contributions.
     */
    List<Contribution> getContributions() throws Exception;

    /**
     * Gets a contribution given its name.
     */
    Contribution getContribution(String name) throws Exception;

    /**
     * Persists a new contribution. The contribution will not be installed. You
     * need to explicitly call {@link #installContribution(Contribution)} to
     * install the contribution.
     */
    Contribution addContribution(Contribution contrib) throws Exception;

    /**
     * Removes a persisted contribution given its name. The contribution will not
     * be uninstalled before being removed. You need to explicitly call
     * {@link #uninstallContribution(Contribution)} to uninstall it.
     *
     * @return true if the contribution was removed, false if the contribution
     *         was not found in persistence.
     */
    boolean removeContribution(Contribution contrib) throws Exception;

    /**
     * Installs the contribution given its name. Return true if contribution
     * install succeeds, false if the contribution is already installed.
     * <p>
     * To be able to install a contribution you need to persist it first.
     */
    boolean installContribution(Contribution contrib) throws Exception;

    /**
     * Uninstalls a contribution given is name. If not already installed return
     * false otherwise return true. The contribution persisted state is not
     * modified by this operation.
     */
    boolean uninstallContribution(Contribution contrib) throws Exception;

    /**
     * Updates in the storage the given contribution modifications.
     * <p>
     * A contribution cannot be renamed. The only permitted modifications are
     * changing the description and the auto start status.
     * <p>
     * Return back the contribution object.
     */
    Contribution updateContribution(Contribution contribution) throws Exception;

    /**
     * Checks whether a contribution is currently installed.
     */
    boolean isInstalled(Contribution contrib) throws Exception;

    /**
     * Checks whether a contribution is currently persisted.
     */
    boolean isPersisted(Contribution contrib) throws Exception;

    /**
     * Starts the service. This will install all persisted contributions that are
     * marked as auto-install. See {@link Contribution#isDisabled()}
     */
    void start() throws Exception;

    /**
     * Stops the service. This will uninstall all installed contributions.
     */
    void stop() throws Exception;

}
