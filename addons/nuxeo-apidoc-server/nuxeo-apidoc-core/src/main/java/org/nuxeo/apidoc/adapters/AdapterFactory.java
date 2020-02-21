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
package org.nuxeo.apidoc.adapters;

import org.nuxeo.apidoc.api.BundleGroup;
import org.nuxeo.apidoc.api.BundleInfo;
import org.nuxeo.apidoc.api.ComponentInfo;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.ExtensionInfo;
import org.nuxeo.apidoc.api.ExtensionPointInfo;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.OperationInfo;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.api.ServiceInfo;
import org.nuxeo.apidoc.api.graph.Graph;
import org.nuxeo.apidoc.documentation.DocumentationItemDocAdapter;
import org.nuxeo.apidoc.repository.RepositoryDistributionSnapshot;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory for DocumentModel adapters.
 */
public class AdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> adapterClass) {

        if (doc == null) {
            return null;
        }

        String adapterClassName = adapterClass.getSimpleName();

        if (adapterClassName.equals(BundleGroup.class.getSimpleName())) {
            if (doc.getType().equals(BundleGroup.TYPE_NAME)) {
                return new BundleGroupDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(BundleInfo.class.getSimpleName())) {
            if (doc.getType().equals(BundleInfo.TYPE_NAME)) {
                return new BundleInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(ComponentInfo.class.getSimpleName())) {
            if (doc.getType().equals(ComponentInfo.TYPE_NAME)) {
                return new ComponentInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(ExtensionPointInfo.class.getSimpleName())) {
            if (doc.getType().equals(ExtensionPointInfo.TYPE_NAME)) {
                return new ExtensionPointInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(ExtensionInfo.class.getSimpleName())) {
            if (doc.getType().equals(ExtensionInfo.TYPE_NAME)) {
                return new ExtensionInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(DistributionSnapshot.class.getSimpleName())) {
            if (doc.getType().equals(DistributionSnapshot.TYPE_NAME)) {
                return new RepositoryDistributionSnapshot(doc);
            }
        }

        if (adapterClassName.equals(DocumentationItem.class.getSimpleName())) {
            if (doc.getType().equals(DocumentationItem.TYPE_NAME)) {
                return new DocumentationItemDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(ServiceInfo.class.getSimpleName())) {
            if (doc.getType().equals(ServiceInfo.TYPE_NAME)) {
                return new ServiceInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(SeamComponentInfo.class.getSimpleName())) {
            if (doc.getType().equals(SeamComponentInfo.TYPE_NAME)) {
                return new SeamComponentInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(OperationInfo.class.getSimpleName())) {
            if (doc.getType().equals(OperationInfo.TYPE_NAME)) {
                return new OperationInfoDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(Graph.class.getSimpleName())) {
            if (doc.getType().equals(Graph.TYPE_NAME)) {
                return new GraphDocAdapter(doc);
            }
        }

        if (adapterClassName.equals(NuxeoArtifact.class.getSimpleName())) {
            if (doc.getType().equals(ServiceInfo.TYPE_NAME)) {
                return new ServiceInfoDocAdapter(doc);
            }
            if (doc.getType().equals(ExtensionInfo.TYPE_NAME)) {
                return new ExtensionInfoDocAdapter(doc);
            }
            if (doc.getType().equals(ExtensionPointInfo.TYPE_NAME)) {
                return new ExtensionPointInfoDocAdapter(doc);
            }
            if (doc.getType().equals(ComponentInfo.TYPE_NAME)) {
                return new ComponentInfoDocAdapter(doc);
            }
            if (doc.getType().equals(BundleInfo.TYPE_NAME)) {
                return new BundleInfoDocAdapter(doc);
            }
            if (doc.getType().equals(BundleGroup.TYPE_NAME)) {
                return new BundleGroupDocAdapter(doc);
            }
            if (doc.getType().equals(SeamComponentInfo.TYPE_NAME)) {
                return new SeamComponentInfoDocAdapter(doc);
            }
            if (doc.getType().equals(OperationInfo.TYPE_NAME)) {
                return new OperationInfoDocAdapter(doc);
            }
            if (doc.getType().equals(Graph.TYPE_NAME)) {
                return new GraphDocAdapter(doc);
            }
        }

        return null;
    }

}
