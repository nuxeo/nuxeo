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
 * $Id: SearchPrincipal.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query;

import java.io.Serializable;
import java.security.Principal;

/**
 * Search principal.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface SearchPrincipal extends Principal, Serializable {

    /**
     * Return the groups the principal belong to.
     *
     * @return a list of group identifiers.
     */
    String[] getGroups();

    /**
     * Returns tru if user is an administrator
     * <p>
     * Security will still apply to this user
     *
     * @since 5.3 GA
     */
    boolean isAdministrator();

    /**
     * Is the search principal a system user.
     * <p>
     * A system user is a user which doesn't have security restriction at
     * application level. (i.e : only used internally by application components)
     * <p>
     * This is will be helpful for the backend to decide if wether or not it
     * should apply stack security queries.
     *
     * @return true if wether or not this principal is a system principal.
     */
    boolean isSystemUser();

    /**
     * Returns the original principal from which the search principal has been
     * constructed.
     * <p>
     * It returns a Serializable instance since Principal since Principal is not
     * a Serializable.
     * <p>
     * If not constructed from a principal then this method will return null.
     *
     * @return a principal instance.
     */
    Serializable getOriginalPrincipal();

}
