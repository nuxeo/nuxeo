/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ExtendedTaskInstance.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.tasks;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Extended jBPM task instance.
 * <p>
 * Add more parameters needed by the NXWorkflowAPI model.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class ExtendedTaskInstance extends TaskInstance {

    private static final long serialVersionUID = -3172366766256423090L;

    protected String directive;

    protected String comment;

    protected int order = 0;

    protected boolean isRejected = false;


    public boolean isSuspended() {
        // superclass attribute
        return isSuspended;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isRejected() {
        return isRejected;
    }

    public void setRejected(boolean rejected) {
        this.isRejected = rejected;
    }

    public String getDirective() {
        return directive;
    }

    public void setDirective(String directive) {
        this.directive = directive;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
