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

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class TestRandomNodes {

    private static final Log log = LogFactory.getLog(TestRandomNodes.class);

    protected int nbNodes = 0;

    protected int nbFolderish = 0;

    @Test
    public void testBrowse() throws Exception {

        int target = 500 * 1000;

        RandomTextSourceNode root = RandomTextSourceNode.init(target);
        browse(root);
        log.info("browsing completed : " + (nbNodes + nbFolderish) + " nodes visited");
        log.info("   Folderish Nodes : " + (nbFolderish));
        log.info("        Data Nodes : " + (nbNodes));
        // deactivated because non-deterministic and sometimes fails
        // assertTrue("nbNodes=" + nbNodes + " nbFolderish=" + nbFolderish
        // + " target=" + target, nbNodes >= target * 0.1);
    }

    protected void browse(SourceNode node) throws IOException {
        List<SourceNode> children = node.getChildren();
        if (children == null) {
            return;
        }
        log.debug("browsing folder node number " + nbNodes + ": " + node.getName() + " (" + children.size()
                + " children)");
        for (SourceNode child : children) {
            if (child.isFolderish()) {
                nbFolderish++;
                browse(child);
                int level = ((RandomTextSourceNode) child).getLevel();
                assertTrue(level <= RandomTextSourceNode.maxDepth);
            } else {
                nbNodes++;
            }
        }
    }
}
