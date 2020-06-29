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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;

/**
 * @since 11.2
 */
public class Migration {

    protected final String id;

    protected final String description;

    protected final String descriptionLabel;

    protected final MigrationStatus status;

    protected final List<MigrationStep> steps;

    protected Migration(String id, String description, String label, MigrationStatus status,
            List<MigrationStep> steps) {
        this.id = id;
        this.description = description;
        this.descriptionLabel = label;
        this.steps = steps;
        this.status = status;
    }

    public static Migration from(MigrationDescriptor descriptor, MigrationStatus status) {
        return new Migration(descriptor.getId(), descriptor.getDescription(), descriptor.getDescriptionLabel(), status,
                getAvailableSteps(descriptor, status));
    }

    protected static List<MigrationStep> getAvailableSteps(MigrationDescriptor descriptor, MigrationStatus status) {
        return descriptor.getSteps()
                         .values()
                         .stream()
                         .filter(s -> s.getFromState().equals(status.getState()))
                         .map(MigrationStep::from)
                         .collect(Collectors.toUnmodifiableList());
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public List<MigrationStep> getSteps() {
        return steps;
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
        return "Migration [id=" + id + ", description=" + description + ", descriptionLabel=" + descriptionLabel
                + ", status=" + status + ", steps=" + steps + "]";
    }

}
