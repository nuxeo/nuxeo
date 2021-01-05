/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;

/**
 * Descriptor for lifecycle state.
 *
 * @since 11.5
 */
@XObject("state")
public class LifeCycleStateDescriptor implements LifeCycleState {

    @XNode("@name")
    protected String name;

    @XNode("@description")
    protected String description;

    @XNode("@initial")
    protected boolean initial;

    @XNodeList(value = "transitions/transition", type = ArrayList.class, componentType = String.class)
    protected List<String> transitions;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<String> getAllowedStateTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    @Override
    public boolean isInitial() {
        return initial;
    }

}
