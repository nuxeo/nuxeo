/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.cmis;

import java.io.IOException;

import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.repository.QueryCapability;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.repository.RepositoryCapabilities;
import org.apache.chemistry.repository.RepositoryInfo;
import org.nuxeo.ecm.webengine.atom.WorkspaceInfo;
import org.nuxeo.ecm.webengine.atom.XMLWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CMISWorkspaceInfo extends WorkspaceInfo {

    protected Repository repository;
    
    public CMISWorkspaceInfo(Repository repository, String id, String title) {
        super (id, "CmisWorkspace", title);
        this.repository = repository;
    }
 
    public Repository getRepository() {
        return repository;
    }
    
    @Override
    protected void writeWorkspaceElements(String wsUrl, XMLWriter xw)
            throws IOException {
        RepositoryInfo info = repository.getInfo();
        RepositoryCapabilities caps = info.getCapabilities();
        QueryCapability qcap = caps.getQueryCapability();
        String fts;
        String qcs;
        switch (qcap) {
        case NONE:
        case METADATA_ONLY:
            qcs = qcap.toString();
            fts = "none";
            break;
        case FULL_TEXT_ONLY:
            qcs = qcap.toString();
            fts = "fulltextonly";
            break;
        case BOTH_COMBINED:
            qcs = "both";
            fts = "fulltext";
            break;
        case BOTH_SEPARATE:
            qcs = "both";
            fts = "fulltextonly";
            break;
        default:
            throw new UnsupportedOperationException();
        }

        xw.element(CMIS.REPOSITORY_INFO)
            .start()
            .element(CMIS.REPOSITORY_ID).content(repository.getId())
            .element(CMIS.REPOSITORY_NAME).content(info.getName())
            .element(CMIS.REPOSITORY_RELATIONSHIP).content("self")
            .element(CMIS.REPOSITORY_DESCRIPTION).content(info.getDescription())
            .element(CMIS.VENDOR_NAME).content(info.getVendorName())
            .element(CMIS.PRODUCT_NAME).content(info.getProductName())
            .element(CMIS.PRODUCT_VERSION).content(info.getProductVersion())
            .element(CMIS.ROOT_FOLDER_ID).content(info.getRootFolderId())
            .element(CMIS.VERSIONS_SUPPORTED).content(info.getVersionSupported())
            .element(CMIS.CAPABILITIES)
                .start()
                .element(CMIS.CAPABILITY_MULTIFILING).content(caps.hasMultifiling())
                .element(CMIS.CAPABILITY_UNFILING).content(caps.hasUnfiling())
                .element(CMIS.CAPABILITY_VERSION_SPECIFIC_FILING).content(caps.hasVersionSpecificFiling())
                .element(CMIS.CAPABILITY_PWC_UPDATEABLE).content(caps.isPWCUpdatable())
                .element(CMIS.CAPABILITY_PWC_SEARCHABLE).content(caps.isPWCSearchable())
                .element(CMIS.CAPABILITY_ALL_VERSIONS_SEARCHABLE).content(caps.isAllVersionsSearchable())
                .element(CMIS.CAPABILITY_QUERY).content(qcs)
                .element(CMIS.CAPABILITY_JOIN).content(caps.getJoinCapability().toString())
                .element(CMIS.CAPABILITY_FULL_TEXT).content(fts)
                .end()
            .element(CMIS.REPOSITORY_SPECIFIC_INFORMATION)
                .start()
                //TODO
                .end()
            .end();
    }
    
    

}
