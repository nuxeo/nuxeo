package org.nuxeo.apidoc.test;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.documentation.XMLContributionParser;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestXMLParser {

    @Test
    public void testParser() throws Exception {

        File xmlFile = FileUtils.getResourceFileFromContext("sample-fragment-contrib.xml");
        String xml = FileUtils.readFile(xmlFile);

        String html = XMLContributionParser.prettyfy(xml);

    }
}
