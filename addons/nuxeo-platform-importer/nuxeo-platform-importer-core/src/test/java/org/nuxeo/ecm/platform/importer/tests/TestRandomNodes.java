/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.importer.tests;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import junit.framework.TestCase;

public class TestRandomNodes extends TestCase {

    private static final Log log = LogFactory.getLog(TestRandomNodes.class);

    protected int nbNodes = 0;

    protected int nbFolderish = 0;

    public void testBrowse() throws Exception {

        int target = 500 * 1000;

        RandomTextSourceNode root = RandomTextSourceNode.init(target);
        browse(root);
        log.info("browsing completed : " + (nbNodes + nbFolderish)
                + " nodes visited");
        log.info("   Folderish Nodes : " + (nbFolderish));
        log.info("        Data Nodes : " + (nbNodes));
        // deactivated because non-deterministic and sometimes fails
        // assertTrue("nbNodes=" + nbNodes + " nbFolderish=" + nbFolderish
        // + " target=" + target, nbNodes >= target * 0.1);
    }

    protected void browse(SourceNode node) {
        List<SourceNode> children = node.getChildren();
        if (children == null) {
            return;
        }
        log.debug("browsing folder node number " + nbNodes + ": "
                + node.getName() + " (" + children.size() + " children)");
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
