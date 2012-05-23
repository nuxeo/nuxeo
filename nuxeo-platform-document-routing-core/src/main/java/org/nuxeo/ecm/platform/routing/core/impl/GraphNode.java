/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.Set;

import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * A node for a route graph. Represents operation chains, associated task and
 * form, output transitions and their conditions, etc.
 *
 * @since 5.6
 */
public interface GraphNode {

    /**
     * The internal state of a node.
     */
    enum State {
        /** Node is ready. */
        READY("ready", "toReady"),
        /** Merge node is waiting for more incoming transitions. */
        WAITING("waiting", "toWaiting"),
        /** While executing input phase. Not persisted. */
        RUNNING_INPUT,
        /** Task node is waiting for task to be done. */
        SUSPENDED("suspended", "toSuspended"),
        /** While executing output phase. Not persisted. */
        RUNNING_OUTPUT;

        private final String lifeCycleState;

        private final String transition;

        private State() {
            lifeCycleState = null;
            transition = null;
        }

        private State(String lifeCycleState, String transition) {
            this.lifeCycleState = lifeCycleState;
            this.transition = transition;
        }

        /**
         * Corresponding lifecycle state.
         */
        public String getLifeCycleState() {
            return lifeCycleState;
        }

        /**
         * Transition leading to this state.
         */
        public String getTransition() {
            return transition;
        }

        public static State fromString(String s) {
            try {
                return State.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(s);
            }
        }
    }

    /**
     * Get the node id.
     *
     * @return the node id
     */
    String getId();

    /**
     * Get the node state.
     *
     * @return the node state
     */
    State getState();

    /**
     * Set the node state.
     *
     * @param state the node state
     */
    void setState(State state);

    /**
     * Checks if this is the start node.
     */
    boolean isStart();

    /**
     * Checks if this is a stop node.
     */
    boolean isStop();

    /**
     * Checks if this is a merge node.
     */
    boolean isMerge();

    /**
     * Get input chain.
     *
     * @return the input chain
     */
    String getInputChain();

    /**
     * Get output chain.
     *
     * @return the output chain
     */
    String getOutputChain();

    /**
     * Checks it this node has an associated user task.
     */
    boolean hasTask();

    /**
     * Increments the execution counter for this node.
     */
    void incrementCount();

    /**
     * Executes an Automation chain in the context of this node.
     *
     * @param chainId the chain
     */
    void executeChain(String chainId) throws DocumentRouteException;

    Set<String> evaluateTransitionConditions() throws DocumentRouteException;

}
