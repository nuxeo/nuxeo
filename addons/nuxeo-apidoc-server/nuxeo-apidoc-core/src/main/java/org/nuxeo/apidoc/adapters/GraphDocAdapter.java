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
package org.nuxeo.apidoc.adapters;

import java.io.Serializable;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * Persister for distribution graphs.
 *
 * @since 11.1
 */
public class GraphDocAdapter extends BaseNuxeoArtifactDocAdapter implements Graph {

    protected GraphDocAdapter(DocumentModel doc) {
        super(doc);
    }

    // artifact id with type-specific prefix
    @Override
    public String getId() {
        return ARTIFACT_PREFIX + getName();
    }

    @Override
    public String getType() {
        return (String) getDoc().getPropertyValue(PROP_GRAPH_TYPE);
    }

    @Override
    public String getName() {
        return getDoc().getName();
    }

    @Override
    public String getTitle() {
        return (String) getDoc().getPropertyValue(NuxeoArtifact.TITLE_PROPERTY_PATH);
    }

    @Override
    public String getDescription() {
        return (String) getDoc().getPropertyValue(NuxeoArtifact.DESCRIPTION_PROPERTY_PATH);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public Blob getBlob() {
        return (Blob) getDoc().getPropertyValue(Graph.CONTENT_PROPERTY_PATH);
    }

    public static GraphDocAdapter create(Graph graph, CoreSession session, String containerPath) {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName(graph.getName());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }
        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue(Graph.TITLE_PROPERTY_PATH, graph.getTitle());
        doc.setPropertyValue(Graph.DESCRIPTION_PROPERTY_PATH, graph.getDescription());
        doc.setPropertyValue(Graph.PROP_GRAPH_TYPE, graph.getType());
        doc.setPropertyValue(Graph.CONTENT_PROPERTY_PATH, (Serializable) graph.getBlob());

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }
        return new GraphDocAdapter(doc);
    }

}
