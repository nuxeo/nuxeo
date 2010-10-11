/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.web.sign;

import java.io.Serializable;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Certificate management actions
 * Used for generating and storing user key and certificate information
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
@Name("certActions")
@Scope(ScopeType.CONVERSATION)
public class CertActions implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final Log log = LogFactory.getLog(CertActions.class);

    @In(create = true)
    protected transient CertService certService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true)
    protected transient UserManager userManager;

    public void createCert() throws ClientException {
        X509Certificate certificate = certService.getCertificate(getUserInfo());
        NuxeoPrincipal nuxeoPrincipal  = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        log.info(nuxeoPrincipal.getModel().getName());
    }

    UserInfo getUserInfo() throws CertException {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields.put(CNField.UserID, currentUser.getName());
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }
}