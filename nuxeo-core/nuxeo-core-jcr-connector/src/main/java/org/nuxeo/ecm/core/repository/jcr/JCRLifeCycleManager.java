/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JCRLifeCycleManager.java 19318 2007-05-24 18:48:39Z fguillaume $
 */

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleManager;
import org.nuxeo.ecm.core.model.Document;

/**
 * JCR Life Cycle Manager.
 *
 * <p>
 * Deals with the storage of the life cycle properties within a dedicated JCR
 * node on the document itself.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class JCRLifeCycleManager implements LifeCycleManager {

    // :XXX: Check if we should take care of synchronization at this level.

    public void setState(Document doc, String stateName)
            throws LifeCycleException {
        setCurrentLifeCycleState(doc, stateName);
    }

    public String getState(Document doc) throws LifeCycleException {
        String state;
        Node docNode = ((JCRDocument) doc).getNode();
        Node lifeCycleNode = getLifeCycleNode(docNode, false);
        try {
            state = lifeCycleNode.getProperty(
                    NodeConstants.ECM_LIFECYCLE_STATE.rawname).getString();
        } catch (PathNotFoundException e) {
            state = null;
        } catch (Exception e) {
            throw new LifeCycleException("Failed getting life cycle state", e);
        }
        return state;
    }

    public String getPolicy(Document doc) throws LifeCycleException {
        String policy;
        Node docNode = ((JCRDocument) doc).getNode();
        Node lifeCycleNode = getLifeCyclePolicyNode(docNode, false);
        try {
            policy = lifeCycleNode.getProperty(
                    NodeConstants.ECM_LIFECYCLE_POLICY.rawname).getString();
        } catch (PathNotFoundException e) {
            policy = null;
        } catch (Exception e) {
            throw new LifeCycleException("Failed getting life cycle policy", e);
        }
        return policy;
    }

    public void setPolicy(Document doc, String policy)
            throws LifeCycleException {
        setLifeCyclePolicy(doc, policy);
    }

    /**
     * Returns the node where to find the current life cycle name.
     *
     * <p>
     * Override this if you want to store the property differently on the
     * document node.
     * </p>
     *
     * @param docNode : the document node.
     * @param create : create the node if does not exist.
     * @return the (sub)node containing the current life cycle
     */
    protected Node getLifeCycleNode(Node docNode, boolean create) {
        return docNode;
    }

    /**
     * Returns the node where to find the life cycle policy name.
     *
     * <p>
     * Override this if you want to store the property differently on the
     * document node.
     * </p>
     *
     * @param docNode : the document node.
     * @param create : create the node if does not exist.
     * @return the (sub)node containing the life cycle policy name.
     */
    protected Node getLifeCyclePolicyNode(Node docNode, boolean create) {
        return docNode;
    }

    /**
     * Set the current life cycle on a given document.
     *
     * <p>
     * Override this if you want to store the property differently on the
     * document node.
     * </p>
     *
     * @param doc : a Nuxeo core document.
     * @param stateName : the state name.
     * @throws LifeCycleException if write operations failed JCR side.
     */
    protected void setCurrentLifeCycleState(Document doc, String stateName)
            throws LifeCycleException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node lifeCycleNode = getLifeCycleNode(docNode, true);
        if (lifeCycleNode != null) {
            try {
                lifeCycleNode.setProperty(
                        NodeConstants.ECM_LIFECYCLE_STATE.rawname, stateName);
            } catch (RepositoryException re) {
                throw new LifeCycleException("Failed to write life cycle", re);
            }
        }
    }

    /**
     * Set the life cycle policy on a givent document.
     *
     * <p>
     * Override this if you want to store the property differently on the
     * document node.
     * </p>
     *
     * @param doc : a Nuxeo core document.
     * @param policy : the policy name.
     * @throws LifeCycleException if write operations failed JCR side.
     */
    protected void setLifeCyclePolicy(Document doc, String policy)
            throws LifeCycleException {
        Node docNode = ((JCRDocument) doc).getNode();
        Node lifeCycleNode = getLifeCyclePolicyNode(docNode, true);
        if (lifeCycleNode != null) {
            try {
                lifeCycleNode.setProperty(
                        NodeConstants.ECM_LIFECYCLE_POLICY.rawname, policy);
            } catch (RepositoryException re) {
                throw new LifeCycleException(
                        "Failed to write life cycle policy", re);
            }
        }
    }

}
