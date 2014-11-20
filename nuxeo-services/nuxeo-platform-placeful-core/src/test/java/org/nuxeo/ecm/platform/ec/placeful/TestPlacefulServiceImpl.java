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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test the event conf service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class,
        CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.core.persistence",
        "org.nuxeo.ecm.platform.placeful.core" })
@LocalDeploy({ "org.nuxeo.ecm.platform.placeful.core:nxplaceful-ds.xml",
        "org.nuxeo.ecm.platform.placeful.core:nxplaceful-config.xml" })
public class TestPlacefulServiceImpl {

    private static final Log log = LogFactory.getLog(PlacefulServiceImpl.class);

    @Inject
    private PlacefulService placefulService;

    @Test
    public void testRegistration() {
        Map<String, String> registry = placefulService.getAnnotationRegistry();
        assertEquals(1, registry.size());
        assertTrue(registry.containsKey("SubscriptionConfig"));
        assertEquals("org.nuxeo.ecm.platform.ec.placeful.SubscriptionConfig",
                registry.get("SubscriptionConfig"));
    }

    @Test
    public void testAnnotations() {
        SubscriptionConfig config = new SubscriptionConfig();

        config.setEvent("deleted");
        config.setId("000123-023405-045697");
        placefulService.setAnnotation(config);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("event", "deleted");
        paramMap.put("id", "000123-023405-045697");

        List<Annotation> annotations = placefulService.getAnnotationListByParamMap(
                paramMap, "SubscriptionConfig");
        log.info("Nombre d'annotations en bases : " + annotations.size());
        assertTrue(annotations.size() > 0);

        Annotation annotation = placefulService.getAnnotation(
                "000123-023405-045697", "SubscriptionConfig");
        assertNotNull(annotation);
    }

}
