/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.xml;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.connect.update.model.TaskDefinition;
import org.nuxeo.connect.update.task.Task;

/**
 * Describe an install / uninstall task
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject
public class TaskDefinitionImpl implements TaskDefinition {

    /**
     * A class implementing {@link Task}
     */
    @XNode("@class")
    protected String type;

    /**
     * Whether the platform must be restarted after executing the task.
     */
    @XNode("@restart")
    protected boolean restart;

    public TaskDefinitionImpl() {
    }

    public TaskDefinitionImpl(boolean restart) {
        this.restart = restart;
    }

    public TaskDefinitionImpl(String type, boolean restart) {
        this.type = type;
        this.restart = restart;
    }

    public void setRequireRestart(boolean isRestartRequired) {
        restart = isRestartRequired;
    }

    public boolean getRequireRestart() {
        return restart;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
