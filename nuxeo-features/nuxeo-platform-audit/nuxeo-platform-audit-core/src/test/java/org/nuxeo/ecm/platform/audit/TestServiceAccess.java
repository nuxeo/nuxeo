/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Features(AuditFeature.class)
@RunWith(FeaturesRunner.class)
public class TestServiceAccess  {

    @Test
    public void testFullAccess() {
        Logs fullService = Framework.getLocalService(Logs.class);
        assertNotNull(fullService);
    }

    @Test
    public void testReadAccess() {
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        assertNotNull(reader);
    }

    @Test
    public void testWriteAccess() {
        AuditLogger writer = Framework.getLocalService(AuditLogger.class);
        assertNotNull(writer);
    }

}
