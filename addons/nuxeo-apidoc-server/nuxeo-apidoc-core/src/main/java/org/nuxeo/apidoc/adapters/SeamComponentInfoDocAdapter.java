/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.adapters;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;

public class SeamComponentInfoDocAdapter extends BaseNuxeoArtifactDocAdapter
        implements SeamComponentInfo {

    protected SeamComponentInfoDocAdapter(DocumentModel doc) {
        super(doc);
    }

    @Override
    public String getId() {
        return "seam:" + getName();
    }

    @Override
    public String getClassName() {
        return safeGet(PROP_CLASS_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getInterfaceNames() {
        try {
            return (List<String>) doc.getPropertyValue(PROP_INTERFACES);
        } catch (Exception e) {
            log.error("Error while getting service names", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return safeGet(PROP_COMPONENT_NAME);
    }

    @Override
    public String getPrecedence() {
        return safeGet(PROP_PRECEDENCE);
    }

    @Override
    public String getScope() {
        return safeGet(PROP_SCOPE);
    }

    @Override
    public String getArtifactType() {
        return SeamComponentInfo.TYPE_NAME;
    }

    @Override
    public String getVersion() {
        DistributionSnapshot parentSnapshot = getParentNuxeoArtifact(DistributionSnapshot.class);

        if (parentSnapshot == null) {
            log.error("Unable to determine version for bundleGroup " + getId());
            return "?";
        }

        return parentSnapshot.getVersion();
    }

    @Override
    public int compareTo(SeamComponentInfo o) {
        return getClassName().compareTo(o.getClassName());
    }

    public static SeamComponentInfo create(SeamComponentInfo sci,
            CoreSession session, String containerPath) throws Exception {

        DocumentModel doc = session.createDocumentModel(TYPE_NAME);

        String name = computeDocumentName(sci.getId());
        String targetPath = new Path(containerPath).append(name).toString();
        boolean exist = false;
        if (session.exists(new PathRef(targetPath))) {
            exist = true;
            doc = session.getDocument(new PathRef(targetPath));
        }

        doc.setPathInfo(containerPath, name);
        doc.setPropertyValue("dc:title", sci.getName());

        doc.setPropertyValue(PROP_COMPONENT_NAME, sci.getName());
        doc.setPropertyValue(PROP_CLASS_NAME, sci.getClassName());
        doc.setPropertyValue(PROP_SCOPE, sci.getScope());
        doc.setPropertyValue(PROP_INTERFACES,
                (Serializable) sci.getInterfaceNames());

        if (exist) {
            doc = session.saveDocument(doc);
        } else {
            doc = session.createDocument(doc);
        }

        return new SeamComponentInfoDocAdapter(doc);
    }

}
