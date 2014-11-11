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

package org.nuxeo.ecm.platform.ec.placeful;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestPlacefulServiceImpl extends RepositoryOSGITestCase {

    private static final Log log = LogFactory.getLog(PlacefulServiceImpl.class);

    private PlacefulServiceImpl placefulServiceImpl;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.placeful.core");

        deployContrib("org.nuxeo.ecm.platform.placeful.core",
                "nxplacefulservice-configs-tests.xml");
        deployContrib("org.nuxeo.ecm.platform.placeful.core",
        "nxplaceful-tests.xml");

        placefulServiceImpl = (PlacefulServiceImpl) runtime.getComponent(PlacefulService.ID);
        assertNotNull(placefulServiceImpl);

        openRepository();
    }

    protected DocumentModel doCreateDocument() throws ClientException {
        DocumentModel rootDocument = coreSession.getRootDocument();
        DocumentModel model = coreSession.createDocumentModel(
                rootDocument.getPathAsString(), "youps", "File");
        model.setProperty("dublincore", "title", "huum");
        DocumentModel source = coreSession.createDocument(model);
        coreSession.save();
        waitForEventsDispatched();
        return source;
    }

    public void testAnnotations() {
        SubscriptionConfig config = new SubscriptionConfig();
        //DocumentModel source = doCreateDocument();

        config.setEvent("deleted");
        config.setId("000123-023405-045697");
        placefulServiceImpl.setAnnotation(config);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("event", "deleted");
        paramMap.put("id", "000123-023405-045697");

        List<Annotation> annotations = placefulServiceImpl.getAnnotationListByParamMap(paramMap, "SubscriptionConfig");
        log.info("Nombre d'annotations en bases : "+ annotations.size());
        assertTrue(annotations.size() > 0);

        Annotation annotation = placefulServiceImpl.getAnnotation("000123-023405-045697", "SubscriptionConfig");
        assertNotNull(annotation);
    }

}
