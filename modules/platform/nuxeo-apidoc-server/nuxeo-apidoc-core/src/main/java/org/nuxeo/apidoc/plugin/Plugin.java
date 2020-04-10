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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.plugin;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotFilter;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interface for plugins to the {@link SnapshotManager} service, handling specific runtime introspections and their
 * persistence.
 *
 * @since 11.1
 */
public interface Plugin<T extends NuxeoArtifact> {

    /**
     * Returns the plugin unique identifier.
     */
    String getId();

    /**
     * Returns the plugin label, to be displayed in the UI (if not hidden).
     */
    String getLabel();

    /**
     * Returns the plugin webengine type contributed to the main webengine module ((if not hidden).
     * <p>
     * The corresponding class should be annotated with @WebObject and given type.
     * <p>
     * It should extend DefaultObject and implement an #initialize method taking the distribution id as first parameter,
     * and an optional boolean specifiying if the distribution is embedded as an options second parameter.
     */
    String getViewType();

    /**
     * Returns the home view URL for navigation in the UI (if not hidden).
     */
    String getHomeView();

    /**
     * Provides navigation mapping, to handle tab selection in the UI.
     */
    String getView(String url);

    /**
     * Returns the style class to be used in the UI menu (if not hidden).
     */
    String getStyleClass();

    /**
     * Specifies whether the plugin should be displayed in the UI.
     */
    boolean isHidden();

    /**
     * Contributes to the parent object mapper, to handle serialization of local introspection.
     */
    ObjectMapper enrishJsonMapper(ObjectMapper parent);

    /**
     * Returns the plugin snapshot class, needed for json deserialization.
     *
     * @since 11.1
     */
    String getPluginSnapshotClass();

    /**
     * Persists this plugin introspection of the live instance as Nuxeo documents.
     * <p>
     * The plugin live introspection can be retrieved on the given distribution using
     * {@link DistributionSnapshot#getPluginSnapshots()#getId()}.
     * <p>
     * Can do nothing if no persistence is planned.
     */
    void persist(DistributionSnapshot snapshot, CoreSession session, DocumentModel root, SnapshotFilter filter);

    /**
     * Allows initializing the plugin web context, in case a request is needed to introspect resources.
     */
    void initWebContext(DistributionSnapshot snapshot, HttpServletRequest request);

    /**
     * Returns the runtime "live" exploration of resources for this plugin.
     * <p>
     * The plugin live introspection can be retrieved on the given distribution using
     * {@link DistributionSnapshot#getPluginSnapshots()#getId()}.
     */
    PluginSnapshot<T> getRuntimeSnapshot(DistributionSnapshot snapshot);

    /**
     * Returns the persisted exploration of resources for this plugin.
     * <p>
     * Can return null if no persistence is done via
     * {@link #persist(DistributionSnapshot, CoreSession, DocumentModel, SnapshotFilter)}.
     */
    PluginSnapshot<T> getRepositorySnapshot(DocumentModel root);

}
