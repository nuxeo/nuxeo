/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.localfs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

public class LocalFSPublicationTree extends AbstractBasePublicationTree implements PublicationTree {
    private static final long serialVersionUID = 1L;

    public static final String INDEX_FILENAME = "fspublication.index";

    public static final String INDEX_FILENAME_TMP = INDEX_FILENAME + ".tmp";

    @Override
    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName, String title) {
        super.initTree(sid, coreSession, parameters, factory, configName, title);
        try {
            rootNode = new FSPublicationNode(rootPath, getTreeConfigName(), sid);
        } catch (IllegalArgumentException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    protected PublishedDocumentFactory getDefaultFactory() {
        return new FSPublishedDocumentFactory();
    }

    @Override
    protected String getDefaultRootPath() {
        return "/";
    }

    protected void findDocs(List<PublishedDocument> pubDocs, File container, DocumentLocation docLoc) {
        for (File child : container.listFiles()) {
            if (child.isDirectory()) {
                findDocs(pubDocs, child, docLoc);
            } else {
                try {
                    PublishedDocument pubDoc = new FSPublishedDocument(child);
                    if (pubDoc.getSourceRepositoryName().equals(docLoc.getServerName())
                            && pubDoc.getSourceDocumentRef().equals(docLoc.getDocRef())) {
                        pubDocs.add(pubDoc);
                    }
                } catch (NotFSPublishedDocumentException e) {
                    // NOP
                }
            }
        }
    }

    public List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc) {
        List<PublishedDocument> pubDocs = null;
        pubDocs = loadExistingPublishedDocumentFromIndex(docLoc);
        if (pubDocs == null) {
            pubDocs = new ArrayList<PublishedDocument>();
            File root = new File(getPath());
            findDocs(pubDocs, root, docLoc);

            // create the index
            createIndex(pubDocs);
        }
        return pubDocs;
    }

    private List<PublishedDocument> loadExistingPublishedDocumentFromIndex(DocumentLocation docLoc)
            {
        File indexFile = new File(rootPath, INDEX_FILENAME);
        if (!indexFile.exists() || !indexFile.isFile()) {
            return null;
        }

        List<PublishedDocument> pubDocs = new ArrayList<PublishedDocument>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(indexFile));
            String filePath;
            while ((filePath = reader.readLine()) != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    PublishedDocument pubDoc = new FSPublishedDocument(file);
                    if (pubDoc.getSourceRepositoryName().equals(docLoc.getServerName())
                            && pubDoc.getSourceDocumentRef().equals(docLoc.getDocRef())) {
                        pubDocs.add(pubDoc);
                    }
                }
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return pubDocs;
    }

    private void createIndex(List<PublishedDocument> pubDocs) {
        File indexFile = new File(rootPath, INDEX_FILENAME);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(indexFile));
            for (PublishedDocument pubDoc : pubDocs) {
                writer.write(pubDoc.getPath());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void addToIndex(PublishedDocument pubDoc) {
        File fileIndex = new File(rootPath, INDEX_FILENAME);
        File fileIndexTmp = new File(rootPath, INDEX_FILENAME_TMP);

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            if (!fileIndex.exists()) {
                fileIndex.createNewFile();
            }

            reader = new BufferedReader(new FileReader(fileIndex));
            writer = new BufferedWriter(new FileWriter(fileIndexTmp));
            String pathToAdd = pubDoc.getPath();
            String line;
            boolean pathAlreadyFound = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals(pathToAdd)) {
                    pathAlreadyFound = true;
                }
                writer.write(line);
                writer.newLine();
            }
            if (!pathAlreadyFound) {
                writer.write(pathToAdd);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }

            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                }
            }
        }

        // move the tmp index file to the index file after closing both stream
        if (fileIndex.delete()) {
            moveFile(fileIndexTmp, fileIndex);
        }
    }

    private void moveFile(File srcFile, File destFile) {
        try {
            FileUtils.moveFile(srcFile, destFile);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    private void removeFromIndex(PublishedDocument pubDoc) {
        File fileIndex = new File(rootPath, INDEX_FILENAME);
        File fileIndexTmp = new File(rootPath, INDEX_FILENAME_TMP);

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(fileIndex));
            writer = new BufferedWriter(new FileWriter(fileIndexTmp));
            String pathToRemove = pubDoc.getPath();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!pathToRemove.equals(line)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }

            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                }
            }
        }

        // move the tmp index file to the index file after closing both stream
        if (fileIndex.delete()) {
            moveFile(fileIndexTmp, fileIndex);
        }
    }

    public PublicationNode getNodeByPath(String path) {
        return new FSPublicationNode(path, getTreeConfigName(), getSessionId());
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode) {
        PublishedDocument pubDoc = super.publish(doc, targetNode);
        addToIndex(pubDoc);
        return pubDoc;
    }

    @Override
    public PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        PublishedDocument pubDoc = super.publish(doc, targetNode, params);
        addToIndex(pubDoc);
        return pubDoc;
    }

    public void unpublish(DocumentModel doc, PublicationNode targetNode) {
        File container = new File(targetNode.getPath());
        for (File child : container.listFiles()) {
            try {
                unpublish(doc, child);
            } catch (NotFSPublishedDocumentException e) {
                // NOP
            }
        }
    }

    private void unpublish(DocumentModel doc, File file) throws NotFSPublishedDocumentException {
        FSPublishedDocument pubDoc = new FSPublishedDocument(file);
        if (pubDoc.getSourceRepositoryName().equals(doc.getRepositoryName())
                && pubDoc.getSourceDocumentRef().equals(doc.getRef())) {
            new File(pubDoc.getPersistPath()).delete();
            removeFromIndex(pubDoc);
        }
    }

    public void unpublish(PublishedDocument pubDoc) {
        if (!accept(pubDoc)) {
            return;
        }
        FSPublishedDocument fsPublishedDocument = (FSPublishedDocument) pubDoc;
        new File(fsPublishedDocument.getPersistPath()).delete();
        removeFromIndex(pubDoc);
    }

    public void release() {
    }

    protected boolean accept(PublishedDocument publishedDocument) {
        return publishedDocument instanceof FSPublishedDocument;
    }

}
