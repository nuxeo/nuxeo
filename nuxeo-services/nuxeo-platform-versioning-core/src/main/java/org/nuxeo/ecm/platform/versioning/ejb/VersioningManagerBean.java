/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.persistence.Transient;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.platform.versioning.service.VersioningManagerImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * This is a versioning EJB facade.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Stateless
@Local(VersioningManagerLocal.class)
@Remote(VersioningManager.class)
public class VersioningManagerBean implements VersioningManager {

    @Transient
    private VersioningManager service;

    @PostConstruct
    public void ejbCreate() {
        initService();
    }

    @PostActivate
    public void ejbActivate() {
        initService();
    }

    @PrePassivate
    public void ejbPassivate() {
    }

    @Remove
    public void ejbRemove() {
    }

    private void initService() {
        if (service == null) {
            service = (VersioningManager) Framework.getRuntime().getComponent(
                    VersioningManagerImpl.COMPONENT_ID);
        }
    }

    @Override
    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel document)
            throws ClientException {
        return service.getVersionIncEditOptions(document);
    }

    @Override
    public String getVersionLabel(DocumentModel document)
            throws ClientException {
        return service.getVersionLabel(document);
    }

    @Override
    @Deprecated
    public DocumentModel incrementMajor(DocumentModel document)
            throws ClientException {
        return service.incrementMajor(document);
    }

    @Override
    @Deprecated
    public DocumentModel incrementMinor(DocumentModel document)
            throws ClientException {
        return service.incrementMinor(document);
    }

    @Override
    @Deprecated
    public String getMajorVersionPropertyName(String documentType) {
        return service.getMajorVersionPropertyName(documentType);
    }

    @Override
    @Deprecated
    public String getMinorVersionPropertyName(String documentType) {
        return service.getMinorVersionPropertyName(documentType);
    }

}
