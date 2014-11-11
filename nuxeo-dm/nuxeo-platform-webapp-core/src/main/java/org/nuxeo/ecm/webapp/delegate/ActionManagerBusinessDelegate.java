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

package org.nuxeo.ecm.webapp.delegate;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.annotation.security.PermitAll;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("actionManager")
@Scope(CONVERSATION)
public class ActionManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(ActionManagerBusinessDelegate.class);

    protected ActionManager actionManager;

    // @Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link ActionManager} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     */
    @Unwrap
    public ActionManager getActionManager() {
        if (null == actionManager) {

            // Access directly the Runtime Service !!!
            // from the web layer, it is useful to have conditions that can use
            // the SeamContext because SeamContext is not serializable, it
            // can't be passed to the ActionManagerBean ejb interface
            //
            // This means the Action Service must be deployed on the same JVM
            // than the WebLayer
            // This is not a problem since that's what we want
            //
            // Remote EJB3 interface is still available, but won't be able to
            // resolve Seam based EL

            RuntimeService runtime = Framework.getRuntime();
            if (runtime != null) {
                actionManager = (ActionManager) runtime.getComponent(ActionService.ID);
            }
        }

        return actionManager;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        if (null != actionManager) {
            actionManager.remove();
            actionManager = null;
        }
    }

}
