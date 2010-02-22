/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.ws;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;

import org.apache.chemistry.ws.CmisException;
import org.apache.chemistry.ws.CmisExtensionType;
import org.apache.chemistry.ws.CmisRepositoryCapabilitiesType;
import org.apache.chemistry.ws.CmisRepositoryEntryType;
import org.apache.chemistry.ws.CmisRepositoryInfoType;
import org.apache.chemistry.ws.CmisTypeContainer;
import org.apache.chemistry.ws.CmisTypeDefinitionListType;
import org.apache.chemistry.ws.CmisTypeDefinitionType;
import org.apache.chemistry.ws.EnumCapabilityJoin;
import org.apache.chemistry.ws.EnumCapabilityQuery;
import org.apache.chemistry.ws.RepositoryServicePort;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * @author Florent Guillaume
 */
@WebService(name = "RepositoryServicePort", //
targetNamespace = "http://docs.oasis-open.org/ns/cmis/ws/200908/", //
serviceName = "RepositoryService", //
portName = "RepositoryServicePort", //
endpointInterface = "org.apache.chemistry.ws.RepositoryServicePort")
public class RepositoryServicePortImpl implements RepositoryServicePort {

    public List<CmisRepositoryEntryType> getRepositories(
            CmisExtensionType extension) throws CmisException {
        CmisRepositoryEntryType repositoryEntryType = new CmisRepositoryEntryType();
        String repositoryName = "default"; // TODO hardcoded
        repositoryEntryType.setRepositoryId(repositoryName);
        repositoryEntryType.setRepositoryName(repositoryName);
        return Collections.singletonList(repositoryEntryType);
    }

    public CmisRepositoryInfoType getRepositoryInfo(String repositoryId,
            CmisExtensionType extension) throws CmisException {
        final CmisRepositoryInfoType repositoryInfo = new CmisRepositoryInfoType();

        try {
            new UnrestrictedSessionRunner(repositoryId) {
                @Override
                public void run() throws ClientException {
                    repositoryInfo.setRootFolderId(session.getRootDocument().getId());
                }
            }.runUnrestricted();
        } catch (ClientException e) {
            throw new CmisException(e.getMessage(), null, e);
        }

        repositoryInfo.setRepositoryId(repositoryId);
        repositoryInfo.setRepositoryName(repositoryId);
        repositoryInfo.setRepositoryDescription(repositoryId);
        repositoryInfo.setVendorName("Nuxeo");
        repositoryInfo.setProductName("Nuxeo");
        repositoryInfo.setProductVersion("5.3.1-SNAPSHOT");
        repositoryInfo.setCmisVersionSupported("1.0");

        CmisRepositoryCapabilitiesType capabilities = new CmisRepositoryCapabilitiesType();
        capabilities.setCapabilityMultifiling(false);
        capabilities.setCapabilityUnfiling(false);
        capabilities.setCapabilityVersionSpecificFiling(false);
        capabilities.setCapabilityPWCUpdatable(false);
        capabilities.setCapabilityPWCSearchable(false);
        capabilities.setCapabilityAllVersionsSearchable(false);
        capabilities.setCapabilityQuery(EnumCapabilityQuery.METADATAONLY);
        capabilities.setCapabilityJoin(EnumCapabilityJoin.INNERANDOUTER);
        repositoryInfo.setCapabilities(capabilities);

        return repositoryInfo;
    }

    public CmisTypeDefinitionType getTypeDefinition(String repositoryId,
            String typeId, CmisExtensionType extension) throws CmisException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CmisTypeDefinitionListType getTypeChildren(String repositoryId,
            String typeId, Boolean includePropertyDefinitions,
            BigInteger maxItems, BigInteger skipCount,
            CmisExtensionType extension) throws CmisException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<CmisTypeContainer> getTypeDescendants(String repositoryId,
            String typeId, BigInteger depth,
            Boolean includePropertyDefinitions, CmisExtensionType extension)
            throws CmisException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
