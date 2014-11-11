/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.storage.sql;

/**
 * A {@link Mapper} that cache rows.
 */
public interface CachingMapper extends Mapper {

    /**
     * Initialize the caching mapper instance
     *
     */
    public void initialize(Model model, Mapper mapper,
            InvalidationsPropagator cachePropagator,
            InvalidationsPropagator eventPropagator,
            InvalidationsQueue repositoryEventQueue);

    /**
     * Sets the session, used for event propagation.
     */
    public void setSession(SessionImpl session);

}
