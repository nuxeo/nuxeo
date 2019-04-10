/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.registry.context.DepthValues.root;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class TestRandomNodes {

    private static final Log log = LogFactory.getLog(TestRandomNodes.class);

    protected int nbDataNodes = 0;

    protected int nbFolderishNodes = 0;

    protected int defaultSmallNbNodes = 1 + RandomTextSourceNode.defaultNbDataNodesPerFolder
            / RandomTextSourceNode.smallNbNodesDivider;

    protected int defaultBigNbNodes = 1 + RandomTextSourceNode.defaultNbDataNodesPerFolder
            * RandomTextSourceNode.bigNbNodesFactor;

    protected boolean isSmallNbNodes = false;

    protected boolean isBigNbNodes = false;

    @Test
    public void testRandomWithProperties() throws Exception {
        RandomTextSourceNode root = RandomTextSourceNode.init(1000, 0, true, false, true, "en_US");
        List<SourceNode> children = root.getChildren();
        for (SourceNode child: children) {
            if (child.isFolderish()) {
                assertNull(child.getBlobHolder().getBlob());
            } else {
                assertEquals(0, child.getBlobHolder().getBlob().getLength());
            }
            assertNotNull(child.getBlobHolder().getProperty("dc:source"));
            assertNotNull(child.getBlobHolder().getProperty("dc:coverage"));
        }
    }

    @Test
    public void testBrowse() throws Exception {

        int target = 500 * 1000;

        RandomTextSourceNode root = RandomTextSourceNode.init(target);
        browse(root);
        logReport();
        // deactivated because non-deterministic and sometimes fails
        // assertTrue("nbNodes=" + nbNodes + " nbFolderish=" + nbFolderish
        // + " target=" + target, nbNodes >= target * 0.1);
        assertFalse(isBigNbNodes);
    }


    @Test
    public void testNonUniformDistribution() throws IOException {

        int target = 500 * 1000;

        RandomTextSourceNode root = RandomTextSourceNode.init(target, null, true, true, false, null);
        browse(root);
        logReport();
        assertTrue(isSmallNbNodes);
        assertTrue(isBigNbNodes);
    }

    protected void browse(SourceNode node) throws IOException {
        List<SourceNode> children = node.getChildren();
        if (children == null) {
            return;
        }
        int childrenCount = children.size();
        if (!isSmallNbNodes && childrenCount > 0 && childrenCount < defaultSmallNbNodes + 1) {
            log.info("Found a node with a small number of children: " + childrenCount);
            isSmallNbNodes = true;
        }
        if (!isBigNbNodes && childrenCount > defaultBigNbNodes - 1) {
            log.info("Found a node with a big number of children: " + childrenCount);
            isBigNbNodes = true;
        }
        log.info("Number of visited nodes = " + (nbDataNodes + nbFolderishNodes) + "; Browsing folderish node number "
                + nbFolderishNodes + ": " + node.getName() + " (" + childrenCount + " children)");
        for (SourceNode child : children) {
            if (child.isFolderish()) {
                nbFolderishNodes++;
                browse(child);
                int level = ((RandomTextSourceNode) child).getLevel();
                assertTrue(level <= RandomTextSourceNode.maxDepth);
            } else {
                nbDataNodes++;
            }
        }
    }

    protected void logReport() {
        log.info("Browsing completed: " + (nbDataNodes + nbFolderishNodes) + " nodes visited");
        log.info("   Folderish nodes: " + (nbFolderishNodes));
        log.info("        Data nodes: " + (nbDataNodes));
    }
}
