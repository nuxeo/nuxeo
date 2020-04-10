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
import org.nuxeo.apidoc.repository.UnrestrictedRootCreator;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 11.1
 */
public abstract class AbstractPlugin<T extends NuxeoArtifact> implements Plugin<T> {

    protected final PluginDescriptor descriptor;

    public AbstractPlugin(PluginDescriptor descriptor) {
        super();
        this.descriptor = descriptor;
    }

    @Override
    public String getId() {
        return descriptor.getId();
    }

    @Override
    public String getPluginSnapshotClass() {
        return descriptor.getSnapshotClass();
    }

    @Override
    public String getLabel() {
        return descriptor.getLabel();
    }

    @Override
    public String getViewType() {
        return descriptor.getViewType();
    }

    @Override
    public String getHomeView() {
        return descriptor.getHomeView();
    }

    @Override
    public String getStyleClass() {
        return descriptor.getStyleClass();
    }

    @Override
    public ObjectMapper enrishJsonMapper(ObjectMapper parent) {
        // NOOP
        return parent;
    }

    protected DocumentModel getOrCreateSubRoot(CoreSession session, DocumentModel root, String name) {
        DocumentRef rootRef = new PathRef(root.getPathAsString() + name);
        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }
        UnrestrictedRootCreator creator = new UnrestrictedRootCreator(session, root.getPathAsString(), name, false);
        creator.runUnrestricted();
        // flush caches
        session.save();
        return session.getDocument(creator.getRootRef());
    }

    @Override
    public void initWebContext(DistributionSnapshot snapshot, HttpServletRequest request) {
        // NOOP
    }

    @Override
    public String getView(String url) {
        // NOOP
        return null;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

}
