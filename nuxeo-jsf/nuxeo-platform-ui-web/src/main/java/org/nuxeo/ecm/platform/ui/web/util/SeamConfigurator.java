/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.runtime.api.Framework;
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

    public boolean isDebugEnabled() {
        return Framework.isBooleanPropertyTrue(ConfigurationGenerator.SEAM_DEBUG_SYSTEM_PROP);
    }

    @Create
    public void init() {
        // FIXME: this init is done too late: debug components have already
        // been scanned and not installed => debug page will not work if
        // available (needs jar jboss-seam-debug to be available)
        init.setDebug(isDebugEnabled());
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
