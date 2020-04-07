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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.plugin.Plugin;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

public interface SnapshotManager {

    /**
     * Initializes the web context, as potentially needed by plugins.
     *
     * @since 11.1
     */
    void initWebContext(HttpServletRequest request);

    DistributionSnapshot getRuntimeSnapshot();

    void addPersistentSnapshot(String key, DistributionSnapshot snapshot);

    DistributionSnapshot getSnapshot(String key, CoreSession session);

    List<DistributionSnapshot> readPersistentSnapshots(CoreSession session);

    List<DistributionSnapshot> listPersistentSnapshots(CoreSession session);

    Map<String, DistributionSnapshot> getPersistentSnapshots(CoreSession session);

    List<String> getPersistentSnapshotNames(CoreSession session);

    List<DistributionSnapshotDesc> getAvailableDistributions(CoreSession session);

    List<String> getAvailableVersions(CoreSession session, NuxeoArtifact nxItem);

    void exportSnapshot(CoreSession session, String key, OutputStream out) throws IOException;

    void importSnapshot(CoreSession session, InputStream is) throws IOException;

    DistributionSnapshot persistRuntimeSnapshot(CoreSession session);

    DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name, Map<String, Serializable> properties);

    DistributionSnapshot persistRuntimeSnapshot(CoreSession session, String name, Map<String, Serializable> properties,
            SnapshotFilter filter);

    void validateImportedSnapshot(CoreSession session, String name, String version, String pathSegment, String title);

    DocumentModel importTmpSnapshot(CoreSession session, InputStream is) throws IOException;

    /**
     * Returns all registered plugins.
     *
     * @since 11.1
     */
    List<Plugin<?>> getPlugins();

    /**
     * Returns the plugin with the given id.
     *
     * @since 11.1
     */
    Plugin<?> getPlugin(String id);

}
