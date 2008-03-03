/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.archive;

/**
 * @author <a href="mailto:bt@nuxeo.com">bogdant</a>
 *
 */
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.ecm.platform.archive.api.ArchiveManager;
import org.nuxeo.ecm.platform.archive.api.ArchiveRecord;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.util.NXRuntimeApplication;

public class ArchiveManagerClient extends NXRuntimeApplication{

    @PersistenceContext(unitName = "NXArchive")
    EntityManager em;

    public static void main(String[] args) {
        new ArchiveManagerClient().start(args);
    }

    @Override
    protected void deployAll() {
        super.deployAll();
        deploy("OSGI-INF/PlatformService.xml");
        deploy("OSGI-INF/PlatformBindings.xml");
    }

    @Override
    protected void run() throws Exception {
        UserSession us = new UserSession("Administrator", "Administrator");
        us.login();
        // ------------ user session started -----------
        ArchiveManager amgr = Framework.getService(ArchiveManager.class);
        amgr.addArchiveRecord(null);

        List<ArchiveRecord> records = amgr.getArchiveRecordsByDocUID("1");
        System.out.print(records.toString());
        // ---------------------------------------------------
        us.logout();
    }

}
