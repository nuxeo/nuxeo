/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.runtime.migration;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.runtime.migration.MigrationDescriptor.MigrationStepDescriptor;

/**
 * @since 11.2
 */
public class MigrationStep {

    protected final String id;

    protected final String description;

    protected final String descriptionLabel;

    protected final String fromState;

    protected final String toState;

    protected MigrationStep(String id, String description, String descriptionLabel, String fromState, String toState) {
        this.id = id;
        this.description = description;
        this.descriptionLabel = descriptionLabel;
        this.fromState = fromState;
        this.toState = toState;
    }

    public static MigrationStep from(MigrationStepDescriptor descriptor) {
        return new MigrationStep(descriptor.getId(), descriptor.getDescription(), descriptor.getDescriptionLabel(),
                descriptor.getFromState(), descriptor.getToState());
    }

    public final String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "MigrationStep [id=" + id + ", description=" + description + ", descriptionLabel=" + descriptionLabel
                + ", fromState=" + fromState + ", toState=" + toState + "]";
    }

}
