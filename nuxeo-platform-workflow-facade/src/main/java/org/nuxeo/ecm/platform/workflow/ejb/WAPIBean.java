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
 * $Id: WAPIBean.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import java.security.Principal;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.api.client.ejb.local.WAPILocal;
import org.nuxeo.ecm.platform.workflow.api.client.ejb.remote.WAPIRemote;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.impl.WAPIImpl;

/**
 * WAPI Session bean.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Remote(WAPIRemote.class)
@Local(WAPILocal.class)
public class WAPIBean extends WAPIImpl implements WAPI {

    private static final long serialVersionUID = -8777528285898381216L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WAPIBean.class);

    @Resource
    EJBContext context;

    public Principal getParticipant() {
        Principal principal;
        try {
            principal = context.getCallerPrincipal();
        } catch (Throwable t) {
            principal = null;
        }
        return principal;
    }

}
