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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * This is a versioning EJB facade.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Stateless
@Local(VersioningManager.class)
@Remote(VersioningManager.class)
public class VersioningManagerBean implements VersioningManager {

    private static final Log log = LogFactory.getLog(VersioningManagerBean.class);

    @Transient
    private VersioningService service;

    @PostConstruct
    public void ejbCreate() {
        log.debug("PostConstruct");
        initService();
    }

    @PostActivate
    public void ejbActivate() {
        log.debug("PostActivate");
        initService();
    }

    @PrePassivate
    public void ejbPassivate() {
        log.debug("PrePassivate");
    }

    @Remove
    public void ejbRemove() {
        log.debug("Remove");
    }

    private void initService() {
        if (service == null) {
            service = ServiceHelper.getVersioningService();
        }
    }

    protected Map<String, Object> getDocumentManagerProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        // :XXX: use constants
        props.put("participant", new UserPrincipal(
                SecurityConstants.ADMINISTRATOR));
        return props;
    }

    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel document)
            throws ClientException {
        return service.getVersionIncEditOptions(document);
    }

    public DocumentModel incrementMajor(DocumentModel document)
            throws ClientException {
        return service.incrementMajor(document);
    }

    public DocumentModel incrementMinor(DocumentModel document)
            throws ClientException {
        return service.incrementMinor(document);
    }

    public String getMajorVersionPropertyName(String documentType) {
        return service.getMajorVersionPropertyName(documentType);
    }

    public String getMinorVersionPropertyName(String documentType) {
        return service.getMinorVersionPropertyName(documentType);
    }

    public String getVersionLabel(DocumentModel document) throws ClientException {
        return service.getVersionLabel(document);
    }

    public SnapshotOptions getCreateSnapshotOption(DocumentModel document)
            throws ClientException {
        return service.getCreateSnapshotOption(document);
    }

}
