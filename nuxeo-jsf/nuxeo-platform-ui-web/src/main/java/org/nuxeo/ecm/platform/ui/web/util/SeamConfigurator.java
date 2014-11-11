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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.util;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.Serializable;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.Init;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component that updates the Seam configuration as needed when app starts
 *
 * @author Thierry Delprat
 */
@Name("NuxeoSeamConfigurator")
@Scope(APPLICATION)
@Startup
public class SeamConfigurator implements Serializable {

    private static final long serialVersionUID = 178687658975L;

    private static final Log log = LogFactory.getLog(SeamConfigurator.class);

    @In(value = "org.jboss.seam.core.init")
    transient Init init;

    @Create
    public void init() {
        init.setJbpmInstalled(false);
        try {
            TransactionHelper.lookupUserTransaction();
            log.info("Activate Seam transaction support");
            init.setTransactionManagementEnabled(true);
        } catch (NamingException e) {
            log.info("Deactivate Seam transaction support (no tx manager)");
            init.setTransactionManagementEnabled(false);
        }
    }

}
