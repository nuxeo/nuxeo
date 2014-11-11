/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.core.persistence",
        "org.nuxeo.ecm.platform.audit" })
@LocalDeploy("org.nuxeo.ecm.platform.audit:nxaudit-ds.xml")
public class AuditFeature extends SimpleFeature {

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        clear();
    }

    protected void clear() {
        boolean started = TransactionHelper.isTransactionActive() == false
                && TransactionHelper.startTransaction();
        try {
            doClear();
        } finally {
            if (started) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    public void doClear() {
        EntityManager em = Framework
            .getService(PersistenceProviderFactory.class)
            .newProvider("nxaudit-logs").acquireEntityManager();
        try {
            em.createNativeQuery("delete from nxp_logs_mapextinfos")
                .executeUpdate();
            em.createNativeQuery("delete from nxp_logs_extinfo")
                .executeUpdate();
            em.createNativeQuery("delete from nxp_logs").executeUpdate();
        } finally {
            em.close();
        }
    }
}
