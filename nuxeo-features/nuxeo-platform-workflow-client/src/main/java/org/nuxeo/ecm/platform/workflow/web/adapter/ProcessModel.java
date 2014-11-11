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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ProcessModel.java 20599 2007-06-16 17:14:31Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.adapter;

import java.io.Serializable;

/**
 * Process model.
 * <p>
 * Holds information about a given process.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ProcessModel extends Serializable {

    /**
     * Returns the process instance identifier bound to the actual document.
     *
     * @return the actual pid or null if the document not bound to any process
     *         instance.
     */
    String getProcessInstanceId();

    /**
     * Returns the process instance name bound to the actual document.
     *
     * @return the actual process instance name if the document is bound to a
     *         process or null if not.
     */
    String getProcessInstanceName();

    /**
     * Returns the name of the process creator.
     *
     * @return the name of the process creator.
     */
    String getProcessInstanceCreatorName();

    /**
     * Returns the status of the process.
     *
     * @return the status if the process.
     */
    String getProcessInstanceStatus();

    /**
     * Returns the modification policy specified by the process.
     *
     * @return the modification policy specified by the process.
     */
    String getModificationPolicy();

    /**
     * Returns the versioning policy specified by the process.
     *
     * @return the versioning policy specified by the process.
     */
    String getVersioningPolicy();

    /**
     * Returns the review type specified by the process if the process is aimed
     * at being a review.
     *
     * @return the review type specified by the process if the process is aimed
     *         at being a review.
     */
    String getReviewType();

    /**
     * Returns the review current level of the process if the process is aimed
     * at being a hierarchical review.
     *
     * @return the review current level of the process if the process is aimed
     *         at being a hierarchical review.
     */
    int getReviewCurrentLevel();

    /**
     * Returns the former review level of the process if the process is aimed at
     * being a hierarchical review.
     *
     * <p>
     * Review former level is useful to know the review direction in case of
     * hierarchical review.
     * </p>
     *
     * @return the review current level of the process if the process is aimed
     *         at being a hierarchical review.
     */
    int getReviewFormerLevel();

}
