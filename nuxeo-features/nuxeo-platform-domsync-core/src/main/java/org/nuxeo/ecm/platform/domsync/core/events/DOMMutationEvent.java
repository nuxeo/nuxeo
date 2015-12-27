/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core.events;

import java.io.Serializable;

/**
 * @author Max Stepanov
 */
public abstract class DOMMutationEvent implements Serializable {

    private static final long serialVersionUID = -2969614822761407105L;

    private final String target;

    protected DOMMutationEvent(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DOMMutationEvent) {
            return target.equals(((DOMMutationEvent) obj).target);
        }
        return false;
    }

    @Override
    public String toString() {
        return "target=" + target;
    }
}
