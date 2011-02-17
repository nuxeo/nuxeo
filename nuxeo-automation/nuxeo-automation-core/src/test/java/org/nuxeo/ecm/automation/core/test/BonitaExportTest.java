/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.core.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.core.doc.BonitaExporter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @since 5.4.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class BonitaExportTest {

    @Inject
    AutomationService service;

    protected String getTestJavaContent(String operationId) throws IOException {
        String filePath = "bonita/" + operationId + ".java.test";
        return getTestFile(filePath);
    }

    protected String getTestXMLContent(String operationId) throws IOException {
        String filePath = "bonita/" + operationId + ".xml";
        return getTestFile(filePath);
    }

    protected String getTestFile(String filePath) throws IOException {
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(
                filePath);
        if (fileUrl == null) {
            throw new RuntimeException("File not found: " + filePath);
        }
        return FileUtils.read(new FileInputStream(
                FileUtils.getFilePathFromUrl(fileUrl)));
    }

    protected OperationDocumentation getOperationDoc(String operationId)
            throws Exception {
        return service.getOperation(operationId).getDocumentation();
    }

    @Ignore
    @Test
    public void generateXMLCode() throws Exception {
        String operationId = "Document.Create";
        Assert.assertEquals(getTestXMLContent(operationId),
                BonitaExporter.getXMLDescription(getOperationDoc(operationId)));
    }

    @Test
    public void generateJavaCode() throws Exception {
        String operationId = "Document.Create";
        Assert.assertEquals(getTestJavaContent(operationId),
                BonitaExporter.getJavaClass(getOperationDoc(operationId)));
    }
}
