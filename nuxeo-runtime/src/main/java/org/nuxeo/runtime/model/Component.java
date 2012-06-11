/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model;

import org.nuxeo.runtime.service.TimestampedService;

/**
 * A Nuxeo Runtime component.
 * <p>
 * Components are extensible and adaptable objects and they provide methods to
 * respond to component life cycle events.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Component extends Extensible, TimestampedService {

    /**
     * Activates the component.
     * <p>
     * This method is called by the runtime when a component is activated.
     *
     * @param context the runtime context
     * @throws Exception if an error occurs during activation
     */
    void activate(ComponentContext context) throws Exception;

    /**
     * Deactivates the component.
     * <p>
     * This method is called by the runtime when a component is deactivated.
     *
     * @param context the runtime context
     * @throws Exception if an error occurs during activation
     */
    void deactivate(ComponentContext context) throws Exception;

    /**
     * The component notification order for {@link #applicationStarted}.
     * <p>
     * Components are notified in increasing order. Order 1000 is the default
     * order for components that don't care. Order 100 is the repository
     * initialization.
     *
     * @return the order, 1000 by default
     * @since 5.6
     */
    int getApplicationStartedOrder();

    /**
     * Notify the component that Nuxeo Framework finished starting all Nuxeo
     * bundles.
     *
     * @throws Exception
     */
    void applicationStarted(ComponentContext context) throws Exception;

}
