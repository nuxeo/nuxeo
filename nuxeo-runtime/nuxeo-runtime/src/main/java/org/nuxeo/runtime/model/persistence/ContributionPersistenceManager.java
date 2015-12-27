/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.model.persistence;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ContributionPersistenceManager {

    /**
     * Gets a list with all persisted contributions.
     */
    List<Contribution> getContributions();

    /**
     * Gets a contribution given its name.
     */
    Contribution getContribution(String name);

    /**
     * Persists a new contribution. The contribution will not be installed. You need to explicitly call
     * {@link #installContribution(Contribution)} to install the contribution.
     */
    Contribution addContribution(Contribution contrib);

    /**
     * Removes a persisted contribution given its name. The contribution will not be uninstalled before being removed.
     * You need to explicitly call {@link #uninstallContribution(Contribution)} to uninstall it.
     *
     * @return true if the contribution was removed, false if the contribution was not found in persistence.
     */
    boolean removeContribution(Contribution contrib);

    /**
     * Installs the contribution given its name. Return true if contribution install succeeds, false if the contribution
     * is already installed.
     * <p>
     * To be able to install a contribution you need to persist it first.
     */
    boolean installContribution(Contribution contrib);

    /**
     * Uninstalls a contribution given is name. If not already installed return false otherwise return true. The
     * contribution persisted state is not modified by this operation.
     */
    boolean uninstallContribution(Contribution contrib);

    /**
     * Updates in the storage the given contribution modifications.
     * <p>
     * A contribution cannot be renamed. The only permitted modifications are changing the description and the auto
     * start status.
     * <p>
     * Return back the contribution object.
     */
    Contribution updateContribution(Contribution contribution);

    /**
     * Checks whether a contribution is currently installed.
     */
    boolean isInstalled(Contribution contrib);

    /**
     * Checks whether a contribution is currently persisted.
     */
    boolean isPersisted(Contribution contrib);

    /**
     * Starts the service. This will install all persisted contributions that are marked as auto-install. See
     * {@link Contribution#isDisabled()}
     */
    void start();

    /**
     * Stops the service. This will uninstall all installed contributions.
     */
    void stop();

}
