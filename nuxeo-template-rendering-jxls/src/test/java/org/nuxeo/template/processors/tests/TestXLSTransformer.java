/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.template.processors.tests;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.context.SimpleContextBuilder;

import net.sf.jxls.transformer.XLSTransformer;

/**
 * This test is mainly here to check that we can correctly invoke XLSTransformer and that there is no linkage issue
 * between JXLS and Apache POI.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.dublincore", //
        "org.nuxeo.template.manager.api", //
        "org.nuxeo.template.manager", //
        "org.nuxeo.template.manager.jxls", //
})
@Deploy("org.nuxeo.template.manager.jxls.tests:OSGI-INF/testxlstransformer-config.xml")
public class TestXLSTransformer {

    @Inject
    protected CoreSession session;

    @Test
    public void testXLSTransformer() throws Exception {
        File input = FileUtils.getResourceFileFromContext("data/ProjectRevenue.xlsx");
        File out = Framework.createTempFile("testxls", "");
        try {
            DocumentModel doc = session.createDocumentModel("nxtrProject");
            Map<String, Serializable> people1 = new HashMap<>();
            people1.put("role", "Manager");
            people1.put("number", Long.valueOf(2));
            people1.put("price_per_day", Double.valueOf(12345.0));
            people1.put("number_of_days", Long.valueOf(15));
            doc.setPropertyValue("nxtrproject:involved_people", (Serializable) Arrays.asList(people1));
            Map<String, Object> ctx = new SimpleContextBuilder().build(doc, "ProjectRevenue");
            XLSTransformer transformer = new XLSTransformer();
            transformer.transformXLS(input.getAbsolutePath(), ctx, out.getAbsolutePath());
        } finally {
            out.delete();
        }
    }

}
