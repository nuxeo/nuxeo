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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * A node for a route graph. Represents operation chains, associated task and
 * form, output transitions and their conditions, etc.
 *
 * @since 5.6
 */
public interface GraphNode {

    String MERGE_ONE = "one";

    String MERGE_ALL = "all";

    String PROP_NODE_ID = "rnode:nodeId";

    String PROP_START = "rnode:start";

    String PROP_STOP = "rnode:stop";

    String PROP_MERGE = "rnode:merge";

    String PROP_COUNT = "rnode:count";

    String PROP_CANCELED = "rnode:canceled";

    String PROP_INPUT_CHAIN = "rnode:inputChain";

    String PROP_OUTPUT_CHAIN = "rnode:outputChain";

    String PROP_HAS_TASK = "rnode:hasTask";

    String PROP_VARIABLES_FACET = "rnode:variablesFacet";

    String PROP_TRANSITIONS = "rnode:transitions";

    String PROP_TRANS_NAME = "name";

    String PROP_TRANS_TARGET = "targetId";

    String PROP_TRANS_CONDITION = "condition";

    String PROP_TRANS_RESULT = "result";

    String PROP_TRANS_CHAIN = "chain";

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

    class Transition implements Comparable<Transition> {

        public GraphNode source;

        public MapProperty prop;

        public String id;

        public String condition;

        public String chain;

        public String target;

        public boolean result;

        /** Computed by graph. */
        public boolean loop;

        protected Transition(GraphNode source, Property p)
                throws ClientException {
            this.source = source;
            prop = (MapProperty) p;
            id = (String) prop.get(PROP_TRANS_NAME).getValue();
            condition = (String) prop.get(PROP_TRANS_CONDITION).getValue();
            chain = (String) prop.get(PROP_TRANS_CHAIN).getValue();
            target = (String) prop.get(PROP_TRANS_TARGET).getValue();
        }

        protected void setResult(boolean bool) throws ClientException {
            result = bool;
            prop.get(PROP_TRANS_RESULT).setValue(Boolean.valueOf(bool));
        }

        @Override
        public int compareTo(Transition other) {
            return id.compareTo(other.id);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("id", id).append(
                    "condition", condition).append("result", result).toString();
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
     * Checks if the merge is ready to execute (enough input transitions are
     * present).
     */
    boolean canMerge();

    /**
     * Notes that this node was canceled (increments canceled counter).
     */
    void setCanceled();

    /**
     * Gets the canceled count for this node.
     *
     * @return
     */
    long getCanceledCount();

    /**
     * Cancels the task if this is a suspended task node.
     */
    void cancelTask();

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

    /** Internal during graph init. */
    void initAddInputTransition(Transition transition);

    /**
     * Gets the input transitions.
     */
    List<Transition> getInputTransitions();

    /**
     * Gets the output transitions.
     */
    List<Transition> getOutputTransitions();

    /**
     * Executes an Automation chain in the context of this node for a given
     * transition
     *
     * @param transition the transition
     */
    void executeTransitionChain(Transition transition)
            throws DocumentRouteException;

    /**
     * Evaluates transition conditions and returns the transitions that were
     * true.
     * <p>
     * Transitions are evaluated and ordered by transition id order.
     *
     * @return the true transitions
     */
    List<Transition> evaluateTransitions() throws DocumentRouteException;

    /**
     * Sets the graph and node variables.
     *
     * @param map the map of variables
     */
    void setAllVariables(Map<String, Object> map);

}
