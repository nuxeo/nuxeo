/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import javax.persistence.EntityManager;

import org.junit.runner.RunWith;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test the {@link AuditChangeFinder}.
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common")
public class TestAuditFileSystemChangeFinder extends AbstractChangeFinderTestCase {

    @Override
    protected void cleanUpAuditLog() throws Exception {

        NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        ((DefaultAuditBackend) auditService.getBackend()).getOrCreatePersistenceProvider().run(true, new RunVoid() {
            @Override
            public void runWith(EntityManager em) throws ClientException {
                em.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
                em.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
                em.createNativeQuery("delete from nxp_logs").executeUpdate();
            }
        });
    }

}
