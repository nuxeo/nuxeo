/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public void setRequireRestart(boolean isRestartRequired) {
        restart = isRestartRequired;
    }

    @Override
    public boolean getRequireRestart() {
        return restart;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
