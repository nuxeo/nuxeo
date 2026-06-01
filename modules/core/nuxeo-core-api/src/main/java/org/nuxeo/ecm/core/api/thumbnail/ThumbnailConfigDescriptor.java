/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.api.thumbnail;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for per-repository thumbnail service configuration.
 * <p>
 * Contributed to the {@code configuration} extension point of
 * {@code org.nuxeo.ecm.core.api.thumbnail.ThumbnailService}. The descriptor is keyed by repository name; the descriptor
 * with id {@link #DEFAULT_ID} acts as the global default and is overlaid by per-repository contributions.
 *
 * @since 2025.22
 */
@XObject("thumbnailConfig")
public class ThumbnailConfigDescriptor implements Descriptor {

    /** Identifier used by the default (cross-repository) contribution. */
    public static final String DEFAULT_ID = "default";

    /**
     * Repository name this configuration applies to, or {@link #DEFAULT_ID} for the global default.
     */
    @XNode("@repository")
    protected String repository;

    @XNode("@enabled")
    protected boolean enabled = true;

    @Override
    public String getId() {
        return repository == null ? DEFAULT_ID : repository;
    }

    /**
     * {@code null} means all repositories.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @return true when thumbnails are explicitly disabled for the repository
     */
    public boolean isDisabled() {
        return !enabled;
    }

    @Override
    public ThumbnailConfigDescriptor merge(Descriptor o) {
        var other = (ThumbnailConfigDescriptor) o;
        var merged = new ThumbnailConfigDescriptor();
        merged.repository = getIfNull(other.repository, repository);
        merged.enabled = other.enabled;
        return merged;
    }
}
