package org.nuxeo.ecm.platform.importer.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;

public class RandomTextSourceNode implements SourceNode {

    protected static RandomTextGenerator gen;

    protected static int maxNode = 10000;

    protected static Integer nbNodes = 1;

    protected static Long size;

    protected Random hazard;

    protected String name;

    protected boolean folderish;

    protected static Integer blobSizeInKB;

    public RandomTextSourceNode(boolean folderish) {
        this.folderish = folderish;
        hazard = new Random(System.currentTimeMillis());
    }

    public static RandomTextSourceNode init(int maxSize) throws Exception {
        return init(maxSize, null);
    }


    public static RandomTextSourceNode init(int maxSize, Integer blobSizeInKB) throws Exception {
        gen = new RandomTextGenerator();
        gen.prefilCache();
        maxNode = maxSize;
        nbNodes = 1;
        size = new Long(0);
        RandomTextSourceNode.blobSizeInKB = blobSizeInKB;
        return new RandomTextSourceNode(true);
    }

    public BlobHolder getBlobHolder() {
        if (folderish) {
            return null;
        }
        String content = null;

        if (blobSizeInKB==null) {
            content = gen.getRandomText();
        } else {
            content = gen.getRandomText(blobSizeInKB);
        }
        synchronized (size) {
            size += content.length();
        }
        Blob blob = new StringBlob(content);
        blob.setFilename(getName() + ".txt");
        blob.setMimeType("text/plain");
        return new SimpleBlobHolder(blob);
    }

    protected List<SourceNode> children = null;

    public List<SourceNode> getChildren() {

        if (!folderish) {
            return null;
        }

        if (children!=null) {
            return children;
        }

        if (nbNodes > maxNode) {
            return null;
        }

        children = new ArrayList<SourceNode>();
        int nbChildren = 1+hazard.nextInt(20);

        synchronized (nbNodes) {
            nbNodes = nbNodes + nbChildren;
        }
        boolean hasFolderishChildren = false;

        for (int i = 0; i< nbChildren; i++) {
            int rand = hazard.nextInt(100);
            if (rand % 10==0) {
                children.add(new RandomTextSourceNode(true));
                hasFolderishChildren = true;
            } else {
                children.add(new RandomTextSourceNode(false));
            }
        }
        if (!hasFolderishChildren) {
            children.set(hazard.nextInt(nbChildren), new RandomTextSourceNode(true));
        }
        return children;
    }

    public String getName() {
        if (name==null) {
            if (folderish) {
                name = "folder";
            } else {
                name = "file";
            }
            name = name + "-" + System.currentTimeMillis() + "-" + hazard.nextInt(100);
        }
        return name;
    }

    public boolean isFolderish() {
        return folderish;
    }

    public static Integer getNbNodes() {
        return nbNodes;
    }

    public static Long getSize() {
        return size;
    }

}
