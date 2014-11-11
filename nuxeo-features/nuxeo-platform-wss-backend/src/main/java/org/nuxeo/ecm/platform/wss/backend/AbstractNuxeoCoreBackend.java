/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.wss.backend;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.spi.AbstractWSSBackend;

public abstract class AbstractNuxeoCoreBackend extends AbstractWSSBackend implements NuxeoWSSBackend{

    protected CoreSession session;

    public AbstractNuxeoCoreBackend() {
        super();
    }

    protected CoreSession getCoreSession() throws Exception {

         if (session==null) {
             RepositoryManager rm;
             rm = Framework.getService(RepositoryManager.class);
             session = rm.getDefaultRepository().open();
         }
        return session;
    }

    protected void close() {
        if (session!=null) {
            CoreInstance.getInstance().close(session);
            session=null;
        }
    }

    public void discardChanges() throws WSSException {
        discardChanges(true);
    }

    public void discardChanges(boolean release) throws WSSException {
        if (session!=null) {
            try {
                session.cancel();
                if (release) {
                    close();
                }
            } catch (Exception e) {
                throw new WSSException("Error during discard", e);
            }
        }
    }


    public void saveChanges() throws WSSException {
        saveChanges(true);
    }

    public void saveChanges(boolean release) throws WSSException {
        if (session!=null) {
            try {
                session.save();
                if (release) {
                    close();
                }
            } catch (ClientException e) {
                throw new WSSException("Error during save", e);
            }
        }
    }

}