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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DiffDisplayService} for the default diff display.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({
        "org.nuxeo.diff.core",
        "org.nuxeo.ecm.platform.forms.layout.core:OSGI-INF/layouts-core-framework.xml",
        "org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml",
        "org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml",
        "org.nuxeo.diff.jsf:OSGI-INF/diff-widgets-contrib.xml" })
public class TestDiffDisplayServiceDefaultDisplay extends
        DiffDisplayServiceTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests the default diff display on 2 documents of a different type.
     *
     * @throws ClientException if an error occurs while creating the docs, doing
     *             the diff or getting the dif display blocks
     */
    @Test
    public void testDefaultDiffDisplay() throws ClientException {

        // Create left and right docs
        DocumentModel leftDoc = session.createDocumentModel("Note");
        leftDoc.setPropertyValue("dc:title", "My note");
        leftDoc.setPropertyValue("dc:subjects", new String[] { "Art",
                "Architecture" });
        leftDoc.setPropertyValue("dc:creator", "Joe");
        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("file", new StringBlob("Joe is not rich."));
        file.put("filename", "Joe.txt");
        files.add(file);
        leftDoc.setPropertyValue("files:files", (Serializable) files);
        leftDoc = session.createDocument(leftDoc);

        DocumentModel rightDoc = session.createDocumentModel("File");
        rightDoc.setPropertyValue("dc:title", "My file");
        rightDoc.setPropertyValue("dc:subjects", new String[] { "Art" });
        rightDoc.setPropertyValue("dc:creator", "Jack");
        files = new ArrayList<Map<String, Serializable>>();
        file = new HashMap<String, Serializable>();
        file.put("file", new StringBlob("Joe is not rich, nor is Jack."));
        file.put("filename", "Jack.pdf");
        files.add(file);
        leftDoc.setPropertyValue("files:files", (Serializable) files);
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        // TODO: uncomment if system elements are taken into account
        // assertEquals(3, diffDisplayBlocks.size());
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        for (DiffDisplayBlock diffDisplayBlock : diffDisplayBlocks) {

            /*
             * if (checkDiffDisplayBlock(diffDisplayBlock,
             * "label.diffBlock.system", 1)) {
             * checkDiffDisplayBlockSchema(diffDisplayBlock, "system", 2,
             * Arrays.asList("type", "path")); } else
             */if (checkDiffDisplayBlock(diffDisplayBlock,
                    "label.diffBlock.dublincore", 1)) {
                checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 3,
                        Arrays.asList("title", "subjects", "creator"));
            } else if (checkDiffDisplayBlock(diffDisplayBlock,
                    "label.diffBlock.files", 1)) {
                checkDiffDisplayBlockSchema(diffDisplayBlock, "files", 1,
                        Arrays.asList("files"));
            } else {
                fail("Unmatching diff display block.");
            }
        }
    }

    @Override
    protected boolean checkDiffDisplayBlock(DiffDisplayBlock diffDisplayBlock,
            String label, int schemaCount) {

        // Check label
        if (!label.equals(diffDisplayBlock.getLabel())) {
            return false;
        }

        // Check schema count on left value
        Map<String, Map<String, PropertyDiffDisplay>> value = diffDisplayBlock.getLeftValue();
        if (value == null || schemaCount != value.size()) {
            return false;
        }

        // Check schema count on right value
        value = diffDisplayBlock.getRightValue();
        if (value == null || schemaCount != value.size()) {
            return false;
        }

        // TODO: manage contentDiff

        return true;
    }

    @Override
    protected boolean checkDiffDisplayBlockSchema(
            DiffDisplayBlock diffDisplayBlock, String schemaName,
            int fieldCount, List<String> fieldNames) {

        // Check fields on left value
        Map<String, PropertyDiffDisplay> fields = diffDisplayBlock.getLeftValue().get(
                schemaName);
        if (fields == null || fieldCount != fields.size()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (!fields.containsKey(fieldName)) {
                return false;
            }
        }

        // Check fields on right value
        fields = diffDisplayBlock.getRightValue().get(schemaName);
        if (fields == null || fieldCount != fields.size()) {
            return false;
        }
        for (String fieldName : fieldNames) {
            if (!fields.containsKey(fieldName)) {
                return false;
            }
        }

        // TODO: manage contentDiff

        return true;
    }
}
