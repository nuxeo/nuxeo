/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.runtime.model;

import org.nuxeo.runtime.service.TimestampedService;

/**
 * A Nuxeo Runtime component.
 * <p>
 * Components are extensible and adaptable objects and they provide methods to respond to component life cycle events.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Component extends Extensible, TimestampedService {

    /**
     * Sets the name for this component, as it was defined in its XML.
     * <p>
     * This is called once after construction by the runtime framework.
     *
     * @param name the name
     * @since 10.3
     */
    void setName(String name);

    /**
     * Activates the component.
     * <p>
     * This method is called by the runtime when a component is activated.
     *
     * @param context the runtime context
     */
    void activate(ComponentContext context);

    /**
     * Deactivates the component.
     * <p>
     * This method is called by the runtime when a component is deactivated.
     *
     * @param context the runtime context
     */
    void deactivate(ComponentContext context);

    /**
     * The component notification order for {@link #applicationStarted}.
     * <p>
     * Components are notified in increasing order. Order 1000 is the default order for components that don't care.
     * Order 100 is the repository initialization.
     *
     * @return the order, 1000 by default
     * @since 5.6
     */
    default int getApplicationStartedOrder() {
        return ComponentStartOrders.DEFAULT;
    }

    /**
     * Notify the component that Nuxeo Framework finished starting all Nuxeo bundles. Implementors must migrate the code
     * of the applicationStarted and move it to {@link Component#start(ComponentContext)} and
     * {@link #stop(ComponentContext)} methods
     *
     * @deprecated since 9.2, since the introduction of {@link Component#start(ComponentContext)} and
     *             {@link #stop(ComponentContext)} methods
     */
    @Deprecated
    default void applicationStarted(ComponentContext context) {
        // do nothing by default
    }

    /**
     * Start the component. This method is called after all the components were resolved and activated
     *
     * @since 9.2
     */
    void start(ComponentContext context);

    /**
     * Stop the component.
     *
     * @since 9.2
     */
    void stop(ComponentContext context) throws InterruptedException;

}
