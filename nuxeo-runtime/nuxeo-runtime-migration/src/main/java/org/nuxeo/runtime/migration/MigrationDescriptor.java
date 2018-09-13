/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.migration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor of a Migration, consisting of States and Steps.
 *
 * @since 9.3
 */
@XObject("migration")
public class MigrationDescriptor implements Descriptor {

    @XObject("state")
    public static class MigrationStateDescriptor implements Descriptor {

        @XNode("@id")
        public String id;

        @XNode("description@label")
        public String descriptionLabel;

        @XNode("description")
        public String description;

        @Override
        public String getId() {
            return id;
        }

        public String getDescriptionLabel() {
            return descriptionLabel;
        }

        public String getDescription() {
            return description;
        }
    }

    @XObject("step")
    public static class MigrationStepDescriptor implements Descriptor {

        @XNode("@id")
        public String id;

        @XNode("@fromState")
        public String fromState;

        @XNode("@toState")
        public String toState;

        @XNode("description@label")
        public String descriptionLabel;

        @XNode("description")
        public String description;

        @Override
        public String getId() {
            return id;
        }

        public String getFromState() {
            return fromState;
        }

        public String getToState() {
            return toState;
        }

        public String getDescriptionLabel() {
            return descriptionLabel;
        }

        public String getDescription() {
            return description;
        }
    }

    @XNode("@id")
    public String id;

    @XNode("description@label")
    public String descriptionLabel;

    @XNode("description")
    public String description;

    @XNode("class")
    public Class<?> klass;

    @XNode("defaultState")
    public String defaultState;

    @XNodeMap(value = "state", key = "@id", type = LinkedHashMap.class, componentType = MigrationStateDescriptor.class)
    public Map<String, MigrationStateDescriptor> states = new LinkedHashMap<>();

    @XNodeMap(value = "step", key = "@id", type = LinkedHashMap.class, componentType = MigrationStepDescriptor.class)
    public Map<String, MigrationStepDescriptor> steps = new LinkedHashMap<>();

    @Override
    public String getId() {
        return id;
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public String getDefaultState() {
        return defaultState;
    }

    public Map<String, MigrationStateDescriptor> getStates() {
        return states;
    }

    public Map<String, MigrationStepDescriptor> getSteps() {
        return steps;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        MigrationDescriptor other = (MigrationDescriptor) o;
        MigrationDescriptor merged = new MigrationDescriptor();
        merged.id = id;
        merged.klass = other.klass != null ? other.klass : klass;
        merged.description = other.description != null ? other.description : description;
        merged.defaultState = other.defaultState != null ? other.defaultState : defaultState;
        merged.descriptionLabel = other.descriptionLabel != null ? other.descriptionLabel : descriptionLabel;
        merged.steps.putAll(steps);
        merged.steps.putAll(other.steps);
        merged.states.putAll(states);
        merged.states.putAll(other.states);
        return merged;
    }

}
