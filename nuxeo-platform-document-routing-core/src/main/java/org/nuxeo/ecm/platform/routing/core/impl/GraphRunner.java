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

import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.State;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;

/**
 * Runs the proper nodes depending on the graph state.
 *
 * @since 5.6
 */
public class GraphRunner extends AbstractRunner implements ElementRunner {

    /**
     * Maximum number of steps we do before deciding that this graph is looping.
     */
    public static final int MAX_LOOPS = 100;

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        try {
            startGraph(session, element);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void startGraph(CoreSession session, DocumentRouteElement element)
            throws ClientException {
        element.setRunning(session);
        GraphRoute graph = (GraphRoute) element;
        boolean done = runGraph(graph, graph.getStartNode());
        if (done) {
            element.setDone(session);
        }
        session.save();
    }

    public void resumeGraph(CoreSession session, GraphRoute graph,
            GraphNode node, Object data) throws ClientException {
        // TODO data
        boolean done = runGraph(graph, node);
        if (done) {
            ((DocumentRouteElement) graph).setDone(session);
        }
        session.save();
    }

    /**
     * Runs the graph starting with the given node.
     *
     * @param graph the graph
     * @param initialNode the initial node to run
     * @return {@code true} if the graph execution is done, {@code false} if
     *         there are still suspended nodes
     */
    protected boolean runGraph(GraphRoute graph, GraphNode initialNode)
            throws DocumentRouteException {
        LinkedList<GraphNode> nodes = new LinkedList<GraphNode>();
        nodes.add(initialNode);
        boolean done = false;
        int count = 0;
        while (!nodes.isEmpty()) {
            GraphNode node = nodes.pop();
            count++;
            if (count > MAX_LOOPS) {
                throw new DocumentRouteException("Execution is looping, node: "
                        + node);
            }
            State jump = null;
            switch (node.getState()) {
            case READY:
                if (node.isMerge()) {
                    jump = State.WAITING;
                } else {
                    jump = State.RUNNING_INPUT;
                }
                break;
            case WAITING:
                // TODO check all the input transitions results to see how many
                // are true, then decide depending on the merge style if this
                // merge happens:
                // TODO if there is no merge yet, leave state to waiting and
                // continue the workflow loop,
                // TODO otherwise do the merge:
                // TODO ... recurse on all nodes from incoming non-loop
                // transitions to cancel them:
                // TODO ... ... set the node’s canceled flag,
                // TODO ... ... if the node was suspended, cancel its related
                // task,
                // TODO ... ... move the node back to state ready,
                // TODO ... set merge node state to running input and continue
                // there.
                break;
            case RUNNING_INPUT:
                node.executeChain(node.getInputChain());
                if (node.hasTask()) {
                    // TODO create task
                    node.setState(State.SUSPENDED);
                    // next node
                } else {
                    jump = State.RUNNING_OUTPUT;
                }
                break;
            case SUSPENDED:
                // TODO set the variables from the task form (this node is the
                // one through which the workflow was resumed),
                jump = State.RUNNING_OUTPUT;
                break;
            case RUNNING_OUTPUT:
                node.executeChain(node.getOutputChain());
                List<Transition> trueTrans = node.evaluateTransitions();
                node.incrementCount();
                node.setState(State.READY);
                if (node.isStop()) {
                    if (!nodes.isEmpty()) {
                        throw new DocumentRouteException(
                                String.format(
                                        "Route %s stopped with still pending nodes: %s",
                                        graph, nodes));
                    }
                    done = true;
                } else {
                    if (trueTrans.isEmpty()) {
                        throw new DocumentRouteException(
                                "No transition evaluated to true from node "
                                        + node);
                    }
                    for (Transition t : trueTrans) {
                        node.executeTransitionChain(t);
                        nodes.addLast(graph.getNode(t.target));
                    }
                }
                break;
            }
            if (jump != null) {
                node.setState(jump);
                // loop again on this node
                count--;
                nodes.addFirst(node);
            }
        }
        return done;
    }

}
