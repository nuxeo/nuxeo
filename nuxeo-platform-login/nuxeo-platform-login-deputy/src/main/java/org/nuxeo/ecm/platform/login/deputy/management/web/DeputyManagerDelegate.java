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

package org.nuxeo.ecm.platform.login.deputy.management.web;

import static org.jboss.seam.ScopeType.*;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManager;
import org.nuxeo.runtime.api.Framework;

@Name("deputyManager")
@Scope(SESSION)
public class DeputyManagerDelegate implements Serializable {

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(DeputyManagerDelegate.class);

    private transient DeputyManager deputyManager;

    @Unwrap
    public DeputyManager factoryForDeputyManager() throws ClientException {
        if (deputyManager == null) {
            try {
                deputyManager = Framework.getService(DeputyManager.class);
            } catch (Exception e) {
                log.error("Unable to create deputyManager service : " + e.getMessage());
                throw new ClientException("Unable to create deputyManager service", e);
            }
        }

        return deputyManager;
    }

}
