package org.nuxeo.ecm.platform.importer.tests;

import java.util.List;

import org.nuxeo.ecm.platform.importer.source.RandomTextSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import junit.framework.TestCase;

public class TestRandomNodes extends TestCase {


    protected int nbNodes = 0;
    protected int nbFolderish = 0;

    public void testBrowse() throws Exception {

        int target = 500*1000;

        RandomTextSourceNode root = RandomTextSourceNode.init(target);
        browse(root);
        System.out.println("************************************************");
        System.out.println("browsing completed : " + (nbNodes+nbFolderish) + " nodes visited");
        System.out.println("   Folderish Nodes : " + (nbFolderish) );
        System.out.println("        Data Nodes : " + (nbNodes) );
        System.out.println("browsing over : " + (nbNodes+nbFolderish) + " nodes visited");

        assertTrue(nbNodes >= target);
    }

    protected void browse(SourceNode node) {
        List<SourceNode> children = node.getChildren();
        if (children==null) {
            return;
        }
        System.out.println("browsing folder node number " + nbNodes + ": " + node.getName() + " (" + children.size() + " children)" );
        for (SourceNode child : children) {
            if (child.isFolderish()) {
                nbFolderish++;
                browse(child);
                int level = ((RandomTextSourceNode)child).getLevel();
                assertTrue(level <= RandomTextSourceNode.maxDepth);
            } else {
                nbNodes++;
            }
        }
    }
}
