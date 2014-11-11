/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.session;

import java.io.Serializable;

/**
 * A stateful session component.
 * <p>
 * A component is instantiate and activated the first time it is requested.
 * It is destroyed when the user session ends.
 * <p>
 * Stateful components are not necessarily thread safe and should be used only
 * from the UserSession thread.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Component extends Serializable {

    /**
     * The component was instantiated by the given session.
     * <p>
     * This method should initialize the component.
     * After returning the component will become visible in the session.
     *
     * @param session the user session that created the component
     * @param name the name under this component is registered.
     *      Can be null for unnamed component.
     *
     * @throws InvalidStateException if the component is not in an appropriate life cycle state
     * @throws SessionException an internal error occurred
     */
    void initialize(UserSession session, String name) throws SessionException;

    /**
     * Destroy this component.
     * This is called by the when the owning session is about to be destroyed.
     * The component should release any allocated resources.
     *
     * @param session the session owning this component
     *
     * @throws InvalidStateException if the component is not in an appropriate life cycle state
     * @throws SessionException an internal error occurred
     */
    void destroy(UserSession session) throws SessionException;

    /**
     * Get the component name if any.
     * A component may be initialized under a name.
     * For singleton components no name is needed so this method might return null.
     *
     * @return the name if any otherwise null
     */
    String getName();

    /**
     * Checks whether this component was initialized and can be used.
     */
    boolean isLive();

}
