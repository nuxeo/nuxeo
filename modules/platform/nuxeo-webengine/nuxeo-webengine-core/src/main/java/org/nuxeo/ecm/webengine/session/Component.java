/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.session;

import java.io.Serializable;

/**
 * A stateful session component.
 * <p>
 * A component is instantiate and activated the first time it is requested. It is destroyed when the user session ends.
 * <p>
 * Stateful components are not necessarily thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Component extends Serializable {

    /**
     * The component was instantiated by the given session.
     * <p>
     * This method should initialize the component. After returning the component will become visible in the session.
     *
     * @param session the user session that created the component
     * @param name the name under this component is registered. Can be null for unnamed component.
     * @throws InvalidStateException if the component is not in an appropriate life cycle state
     * @throws SessionException an internal error occurred
     */
    void initialize(UserSession session, String name) throws SessionException;

    /**
     * Destroy this component. This is called by the when the owning session is about to be destroyed. The component
     * should release any allocated resources.
     *
     * @param session the session owning this component
     * @throws InvalidStateException if the component is not in an appropriate life cycle state
     * @throws SessionException an internal error occurred
     */
    void destroy(UserSession session) throws SessionException;

    /**
     * Get the component name if any. A component may be initialized under a name. For singleton components no name is
     * needed so this method might return null.
     *
     * @return the name if any otherwise null
     */
    String getName();

    /**
     * Checks whether this component was initialized and can be used.
     */
    boolean isLive();

}
