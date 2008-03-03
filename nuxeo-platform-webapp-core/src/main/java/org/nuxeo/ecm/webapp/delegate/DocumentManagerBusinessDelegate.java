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
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.shield.NuxeoJavaBeanErrorHandler;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Acquires a {@link DocumentManager} handle and sticks it into the Seam
 * context.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
@Name("documentManager")
@Scope(CONVERSATION)
@NuxeoJavaBeanErrorHandler
public class DocumentManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 7531855222619160585L;

    private static final Log log = LogFactory.getLog(DocumentManagerBusinessDelegate.class);

    protected CoreSession documentManager;

    @In(create=true, required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create=true, required = false)
    protected NavigationContext navigationContext;

    protected RepositoryLocation oldLocation;

    //@Create
    public void initialize() {
        log.debug("Seam component initialized...");
    }

    //@Begin(join=true)
    @Unwrap
    public CoreSession getDocumentManager() throws ClientException {
        //log.debug("Getting Document Manager");
        return getDocumentManager(currentServerLocation);
    }

    public CoreSession getDocumentManager(RepositoryLocation serverLocation)
            throws ClientException {

    	if (serverLocation==null)
    	{
    		// XXX TD : for some reasons the currentServerLocation is not always injected by Seam
    		// typical reproduction case includes Seam remoting call
    		// ==> pull from factory by hand !
    		if (serverLocation==null)
    			serverLocation = (RepositoryLocation) Component.getInstance("currentServerLocation",true);
    	}

        if (documentManager == null) {
            if (serverLocation == null) {
                log.warn("documentManager could not be retrieved because location is null");
                return null;
            } else {
                try {
                    documentManager = ECM.getPlatform().openRepository(
                            serverLocation.getName());
                    oldLocation = serverLocation;
                    log.debug("documentManager retrieved");
                    return documentManager;
                } catch (Exception e) {
                    final String errMsg = "Error opening repository "
                            + e.getMessage();
                    log.error(errMsg, e);
                    throw new ClientException(errMsg);
                }
            }
        } else {
            // check if the existing DM instance is suitable
            if (null != oldLocation
                    && 0 == oldLocation.compareTo(serverLocation)) {
                return documentManager;
            } else {
                try {
                    documentManager = ECM.getPlatform().openRepository(
                            serverLocation.getName());
                    oldLocation = serverLocation;
                    log.debug("documentManager retrieved");
                    return documentManager;
                } catch (Exception e) {
                    final String errMsg = "Error opening repository "
                            + e.getMessage();
                    log.error(errMsg, e);
                    throw new ClientException(errMsg);
                }
            }
        }
    }

    @Destroy
    @Remove
    @PermitAll
    public void remove() throws ClientException {
        log.debug("Destroying seam component...");
        if (documentManager != null) {
            documentManager.destroy();
            documentManager = null;
        }
    }

}
