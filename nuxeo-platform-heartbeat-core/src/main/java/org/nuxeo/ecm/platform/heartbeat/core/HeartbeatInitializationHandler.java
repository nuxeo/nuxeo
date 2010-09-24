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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.management.storage.DocumentStoreHandler;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
/**
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 *
 */
public class HeartbeatInitializationHandler implements DocumentStoreHandler {


    @Override
    public void onStorageInitialization(CoreSession session, DocumentRef rootletRef) {
        DocumentHeartbeatManager mgr = HeartbeatComponent.defaultComponent.manager;
        mgr.start(HeartbeatManager.DEFAULT_HEARTBEAT_DELAY);
    }

}
