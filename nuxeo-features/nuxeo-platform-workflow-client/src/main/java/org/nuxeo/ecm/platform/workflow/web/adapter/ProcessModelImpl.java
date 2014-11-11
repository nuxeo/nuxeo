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
 * $Id: ProcessModelImpl.java 20775 2007-06-18 21:25:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.adapter;

/**
 * Process Model implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ProcessModelImpl implements ProcessModel {

    private static final long serialVersionUID = 1L;

    protected final String processInstanceId;

    protected final String processInstanceName;

    protected final String processInstanceCreatorName;

    protected final String processInstanceStatus;

    protected final String modificationPolicy;

    protected final String versioningPolicy;

    protected final String reviewType;

    protected final int reviewCurrentLevel;

    protected final int reviewFormerLevel;

/* not used - TODO: remove
    public ProcessModelImpl() {
    }
*/

    public ProcessModelImpl(String processInstanceId,
            String processInstanceName, String processInstanceCreatorName,
            String status, String modificationPolicy, String versioningPolicy,
            String reviewType, int reviewCurrentLevel, int reviewFormerLevel) {
        this.processInstanceId = processInstanceId;
        this.processInstanceName = processInstanceName;
        this.processInstanceCreatorName = processInstanceCreatorName;
        this.processInstanceStatus = status;
        this.modificationPolicy = modificationPolicy;
        this.versioningPolicy = versioningPolicy;
        this.reviewType = reviewType;
        this.reviewCurrentLevel = reviewCurrentLevel;
        this.reviewFormerLevel = reviewFormerLevel;
    }

    public String getModificationPolicy() {
        return modificationPolicy;
    }

    public String getProcessInstanceCreatorName() {
        return processInstanceCreatorName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public int getReviewCurrentLevel() {
        return reviewCurrentLevel;
    }

    public int getReviewFormerLevel() {
        return reviewFormerLevel;
    }

    public String getReviewType() {
        return reviewType;
    }

    public String getVersioningPolicy() {
        return versioningPolicy;
    }

    public String getProcessInstanceStatus() {
        return processInstanceStatus;
    }

}
