/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.uidgen;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy("org.nuxeo.ecm.core:OSGI-INF/test-uidgenerator-contrib.xml")
public class TestUIDGeneratorService {

    @Inject
    protected CoreSession session;

    @Inject
    protected UIDGeneratorService service;

    @Test
    public void testUIDGenerator() throws Exception {
        DocumentModel doc = session.createDocumentModel("Note");
        doc.setPropertyValue("dc:format", "BAR");
        String uid = service.createUID(doc);
        int year = new GregorianCalendar().get(Calendar.YEAR);
        assertEquals("BAR" + year + "00001", uid);
    }

}
