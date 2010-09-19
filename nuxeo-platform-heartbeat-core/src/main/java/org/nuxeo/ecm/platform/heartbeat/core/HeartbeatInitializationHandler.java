/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.platform.heartbeat.core;

import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandler;
/**
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 *
 */
public class HeartbeatInitializationHandler implements DocumentStoreHandler {


    @Override
    public void onStorageInitialization(CoreSession session) {
        DocumentHeartbeatManager mgr = HeartbeatComponent.defaultComponent.manager;
        try {
            mgr.createOrUpdateServer(session, mgr.getMyURI(), new Date(0));
        } catch (ClientException e) {
            throw new Error("Canot initialize heartbeat");
        }
    }

}
