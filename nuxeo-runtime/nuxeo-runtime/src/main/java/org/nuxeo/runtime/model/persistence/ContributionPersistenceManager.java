/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.runtime.model.persistence;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ContributionPersistenceManager {

    /**
     * Get a list with all persisted contributions.
     *
     * @return
     * @throws Exception
     */
    List<Contribution> getContributions() throws Exception;

    /**
     * Get a contribution given its name.
     *
     * @param name
     * @return
     * @throws Exception
     */
    Contribution getContribution(String name) throws Exception;

    /**
     * Persist a new contribution. The contribution will not be installed. You
     * need to explicitly call {@link #installContribution(Contribution)} to
     * install the contribution.
     *
     * @param contrib
     * @return
     * @throws Exception
     */
    Contribution addContribution(Contribution contrib) throws Exception;

    /**
     * Remove a persisted contribution given its name. The contribution will not
     * be uninstalled before being removed. You need to explicitly call
     * {@link #uninstallContribution(String)} to uninstall it.
     *
     * @param name
     * @return true if the contribution was removed, false if the contribution
     *         was not found in persistence.
     * @throws Exception
     */
    boolean removeContribution(Contribution contrib) throws Exception;

    /**
     * Install the contribution given its name. Return true if contribution
     * install succeeds, false if the contribution is already installed.
     *
     * To be able to install a contribution you need to persist it first.
     *
     * @param name
     * @return
     * @throws Exception
     */
    boolean installContribution(Contribution contrib) throws Exception;

    /**
     * Uninstall a contribution given is name. If not already installed return
     * false otherwise return true. The contribution persisted state is not
     * modified by this operation.
     *
     * @param name
     * @return
     * @throws Exception
     */
    boolean uninstallContribution(Contribution contrib) throws Exception;

    /**
     * Update in the storage the given contribution modifications.
     * <p>
     * A contribution cannot be renamed. The only permitted modifications are
     * changing the description and the auto start status.
     * <p>
     * Return back the contribution object.
     *
     * @return
     * @throws Exception
     */
    Contribution updateContribution(Contribution contribution) throws Exception;

    /**
     * Check whether a contribution is currently installed.
     *
     * @param contrib
     * @return
     * @throws Exception
     */
    boolean isInstalled(Contribution contrib) throws Exception;

    /**
     * Check whether a contribution is currently persisted.
     *
     * @param name
     * @return
     * @throws Exception
     */
    boolean isPersisted(Contribution contrib) throws Exception;

    /**
     * Start the service. This will install all persisted contributions that are
     * marked as auto-install. See {@link Contribution#isDisabled()}
     *
     * @see
     */
    void start() throws Exception;

    /**
     * Stop the service this will uninstall all installed contributions.
     */
    void stop() throws Exception;

}
