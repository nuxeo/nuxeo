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
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.State;

/**
 * Runs the proper nodes depending on the graph state.
 *
 * @since 5.6
 */
public class GraphRunner extends AbstractRunner implements ElementRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        try {
            startGraph(session, element);
            session.save();
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void startGraph(CoreSession session, DocumentRouteElement element)
            throws DocumentRouteException {
        GraphRoute graph = (GraphRoute) element;
        // TODO graph.setRunning();
        // TODO event
        runGraph(graph, graph.getStartNode());
    }

    public void resumeGraph(GraphRoute graph, GraphNode node, Object data)
            throws DocumentRouteException {
        // TODO data
        runGraph(graph, node);
        // TODO session.save();
    }

    protected void runGraph(GraphRoute graph, GraphNode initialNode)
            throws DocumentRouteException {
        LinkedList<GraphNode> nodes = new LinkedList<GraphNode>();
        nodes.add(initialNode);
        while (!nodes.isEmpty()) {
            GraphNode node = nodes.pop();
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
                Set<String> targetIds = node.evaluateTransitionConditions();

                // TODO evaluate all the output transition conditions and store
                // their result
                // TODO for each condition that is true, add the target node to
                // the pending nodes
                node.incrementCount();
                node.setState(State.READY);
                if (node.isStop()) {
                    // TODO if the node has a stop flag then stop the workflow
                    // TODO If the workflow is stopped by a stop flag and there
                    // are still pending nodes, then it’s a workflow execution
                    // error (bad workflow definition, there should have been a
                    // merge to cancel other tasks).
                }
                break;
            }
            if (jump != null) {
                node.setState(jump);
                // loop again on this node
                nodes.addFirst(node);
            }
        }
    }

}
