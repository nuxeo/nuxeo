/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.search.api.client.querymodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.search.api" })
@LocalDeploy({ "org.nuxeo.ecm.platform.search.api:OSGI-INF/test-querymodel.xml" })
public class TestQueryModel {

    @Inject
    public CoreSession session;

    @SuppressWarnings("deprecation")
    @Inject
    public QueryModelService queryModelService;

    @SuppressWarnings("deprecation")
    @Test
    public void test() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "foo", "File");
        doc.setPropertyValue("size", Long.valueOf(123));
        session.createDocument(doc);
        session.save();

        QueryModelDescriptor descriptor = queryModelService.getQueryModelDescriptor("testqm");
        assertNotNull(descriptor);
        QueryModel qm = new QueryModel(descriptor);
        Object[] params = new Object[1];
        // Integer[]
        params[0] = new Integer[] { Integer.valueOf(123) };
        DocumentModelList list = qm.getDocuments(session, params);
        assertEquals(1, list.size());
        // Long[]
        params[0] = new Long[] { Long.valueOf(123) };
        list = qm.getDocuments(session, params);
        assertEquals(1, list.size());
        // List
        params[0] = Collections.singletonList(Integer.valueOf(123));
        list = qm.getDocuments(session, params);
        assertEquals(1, list.size());
    }

}
