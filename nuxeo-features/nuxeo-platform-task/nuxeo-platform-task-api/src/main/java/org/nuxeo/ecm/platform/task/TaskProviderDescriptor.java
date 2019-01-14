/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     ldoguin
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.task;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * @since 5.5
 */
@XObject("taskProvider")
public class TaskProviderDescriptor {

    @XNode("@id")
    private String id;

    @XNode("@class")
    private Class<TaskProvider> taskProvider;

    @XNode("@enabled")
    private Boolean enabled = true;

    public TaskProvider getNewInstance() {
        try {
            return taskProvider.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getId() {
        return id;
    }

    public Boolean isEnabled() {
        return enabled;
    }
}
