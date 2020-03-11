/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.importer.stream.producer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.platform.importer.random.HunspellDictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;
import org.nuxeo.importer.stream.message.DocumentMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

/**
 * @since 9.1
 */
public class RandomDocumentMessageProducer extends AbstractProducer<DocumentMessage> {
    private static final Log log = LogFactory.getLog(RandomDocumentMessageProducer.class);

    protected final long nbDocuments;

    protected final BlobInfoFetcher blobInfoFetcher;

    protected boolean countFolderAsDocument = true;

    protected int maxFoldersPerFolder = 50;

    protected int maxDocumentsPerFolder = 10000;

    protected int blobSizeKB = 0;

    protected boolean blobOnlyText = false;

    protected int documentCount = 0;

    protected int folderCount = 0;

    protected final Random rand;

    protected static RandomTextGenerator gen;

    protected static final String[] DC_NATURE = { "article", "acknowledgement", "assessment", "application", "order",
            "contract", "quotation", "fax", "worksheet", "letter", "memo", "note", "notification", "procedure",
            "report", "internshipReport", "pressReview" };

    protected static final String[] DC_SUBJECTS = { "art/architecture", "art/comics", "art/cinema", "art/culture",
            "art/danse", "art/music", "sciences/astronomy", "sciences/biology", "sciences/chemistry", "sciences/math",
            "sciences/physic", "society/ecology", "daily life/gastronomy", "daily life/gardening", "daily life/sport",
            "technology/it" };

    protected static final String[] DC_RIGHTS = { "OpenContentL", "CC-BY-NC", "CC-BY-ND", "FreeArt", "ODbi", "GNUGPL",
            "FreeBSD", "CC0" };

    protected static final String[] DC_LANGUAGE = { "IT", "DE", "FR", "US", "EN" };

    protected static final String[] DC_SOURCE = { "internal", "external", "unknown" };

    protected static final String[] DC_COVERAGE = { "europe/France", "europe/Germany", "europe/Italy", "europe/Spain",
            "oceania/Tonga", "africa/Mali", "asia/Japan", "north-america/United_States_of_America" };

    protected int foldersInCurrentFolderLimit;

    protected int documentInCurrentFolderLimit;

    protected enum DocType {
        Root, Folder, Document
    }

    protected DocType currentType = DocType.Root;

    protected int parentIndex = 0;

    protected List<String> parents = new ArrayList<>();

    protected List<String> folderishChildren = new ArrayList<>();

    protected Set<String> children = new HashSet<>();

    protected int documentInCurrentFolderCount = 0;

    public RandomDocumentMessageProducer(int producerId, long nbDocuments, String lang,
            BlobInfoFetcher blobInfoFetcher) {
        super(producerId);
        this.nbDocuments = nbDocuments;
        rand = ThreadLocalRandom.current();

        synchronized (RandomDocumentMessageProducer.class) {
            if (gen == null) {
                gen = new RandomTextGenerator(new HunspellDictionaryHolder(lang));
                gen.prefilCache();
            }
        }
        this.blobInfoFetcher = blobInfoFetcher;
        log.info("RandomDocumentMessageProducer created, nbDocuments: " + nbDocuments);
    }

    public RandomDocumentMessageProducer setMaxFoldersPerFolder(int max) {
        maxFoldersPerFolder = max;
        return this;
    }

    public RandomDocumentMessageProducer setMaxDocumentsPerFolder(int max) {
        maxDocumentsPerFolder = max;
        return this;
    }

    public RandomDocumentMessageProducer countFolderAsDocument(boolean value) {
        countFolderAsDocument = value;
        return this;
    }

    public RandomDocumentMessageProducer withBlob(int sizeKB, boolean onlyText) {
        this.blobSizeKB = sizeKB;
        this.blobOnlyText = onlyText;
        return this;
    }

    @Override
    public int getPartition(DocumentMessage message, int partitions) {
        return getProducerId() % partitions;
    }

    @Override
    public boolean hasNext() {
        if (countFolderAsDocument) {
            return (documentCount + folderCount) < nbDocuments;
        }
        return documentCount < nbDocuments;
    }

    @Override
    public DocumentMessage next() {
        DocumentMessage ret;
        switch (currentType) {
        case Root:
            ret = createRoot();
            parents.add(ret.getId());
            currentType = DocType.Folder;
            foldersInCurrentFolderLimit = rand.nextInt(maxFoldersPerFolder) + 1;
            break;
        case Folder:
            ret = createFolder(parents.get(parentIndex), children);
            folderishChildren.add(ret.getId());
            children.add(ret.getName());
            if (folderishChildren.size() >= foldersInCurrentFolderLimit) {
                currentType = DocType.Document;
                documentInCurrentFolderCount = 0;
                documentInCurrentFolderLimit = rand.nextInt(maxDocumentsPerFolder);
            }
            break;
        default:
        case Document:
            ret = createDocument(parents.get(parentIndex), children);
            children.add(ret.getName());
            documentInCurrentFolderCount += 1;
            if (documentInCurrentFolderCount > documentInCurrentFolderLimit) {
                parentIndex += 1;
                if (parentIndex >= parents.size()) {
                    parents.clear();
                    parents = folderishChildren;
                    folderishChildren = new ArrayList<>();
                    children = new HashSet<>();
                    parentIndex = 0;
                }
                currentType = DocType.Folder;
                foldersInCurrentFolderLimit = rand.nextInt(maxFoldersPerFolder) + 1;
            }
            break;
        }
        // log.debug(ret.getType() + ": " + ret.getId());
        return ret;
    }

    protected DocumentMessage createRoot() {
        folderCount++;
        return getRandomNodeWithPrefix(String.format("%02d-", getProducerId()), "Folder", "");
    }

    protected DocumentMessage createFolder(String parentPath, Set<String> exclude) {
        DocumentMessage node = getRandomNodeWithExclusion("Folder", parentPath, false, exclude);
        folderCount++;
        return node;
    }

    protected DocumentMessage createDocument(String parentPath, Set<String> exclude) {
        DocumentMessage node = getRandomNodeWithExclusion("File", parentPath, true, exclude);
        documentCount++;
        return node;
    }

    protected DocumentMessage getRandomNodeWithExclusion(String type, String parentPath, boolean withBlob,
            Set<String> exclude) {
        DocumentMessage node = getRandomNode(type, parentPath, withBlob);
        String name = node.getName();
        if (exclude.contains(name)) {
            String newName = name + "-" + rand.nextInt(exclude.size());
            node = DocumentMessage.copy(node, newName);
        }
        return node;
    }

    protected DocumentMessage getRandomNode(String type, String parentPath, boolean withBlob) {
        String title = getTitle();
        String name = getName(title);
        HashMap<String, Serializable> props = getRandomProperties(title);
        DocumentMessage.Builder builder = DocumentMessage.builder(type, parentPath, name).setProperties(props);
        if (withBlob) {
            if (blobInfoFetcher != null) {
                BlobInfo blobInfo = blobInfoFetcher.get(builder);
                if (blobInfo != null) {
                    builder.setBlobInfo(blobInfo);
                    if (blobInfo.mimeType != null) {
                        builder.setType(getDocumentTypeForMimeType(blobInfo.mimeType));
                    }
                }
            } else {
                builder.setBlob(getRandomBlob());
            }
        }
        return builder.build();
    }

    protected String getDocumentTypeForMimeType(String mimeType) {
        if (mimeType.startsWith("image")) {
            return "Picture";
        }
        if (mimeType.startsWith("video")) {
            return "Video";
        }
        return "File";
    }

    protected DocumentMessage getRandomNodeWithPrefix(String prefix, String type, String parentPath) {
        String title = getTitle();
        String name = prefix + getName(title);
        HashMap<String, Serializable> props = getRandomProperties(title);
        DocumentMessage.Builder builder = DocumentMessage.builder(type, parentPath, name).setProperties(props);
        return builder.build();
    }

    protected Blob getRandomBlob() {
        if (blobSizeKB == 0) {
            return null;
        }
        String content = gen.getRandomText(blobSizeKB);
        return Blobs.createBlob(content, getBlobMimeType(), null, getName(getTitle()) + ".txt");
    }

    protected String getBlobMimeType() {
        if (blobOnlyText) {
            return "text/plain";
        } else {
            return "text/partial";
        }
    }

    protected String getName(String title) {
        return title.replaceAll("\\W+", "-").toLowerCase();
    }

    protected String getTitle() {
        return capitalize(gen.getRandomTitle(rand.nextInt(3) + 1).trim());
        // return "f" + folderCount;
    }

    protected String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    protected HashMap<String, Serializable> getRandomProperties(String title) {
        HashMap<String, Serializable> ret = new HashMap<>();
        ret.put("dc:title", title);
        if (rand.nextInt(10) == 1) {
            String description = gen.getRandomTitle(rand.nextInt(5) + 1);
            ret.put("dc:description", capitalize(description));
        }
        ret.put("dc:nature", getGaussian(DC_NATURE));
        ret.put("dc:subjects", (Serializable) Collections.singletonList(getGaussian(DC_SUBJECTS)));
        ret.put("dc:rights", getGaussian(DC_RIGHTS));
        ret.put("dc:language", getGaussian(DC_LANGUAGE));
        ret.put("dc:coverage", getGaussian(DC_COVERAGE));
        ret.put("dc:source", getGaussian(DC_SOURCE));
        return ret;
    }

    protected String getGaussian(String[] words) {
        double g = Math.abs(rand.nextGaussian() / 4);
        g = Math.min(g, 1);
        int i = (int) Math.floor(g * (words.length - 1));
        return words[i];
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (blobInfoFetcher != null) {
            blobInfoFetcher.close();
        }
    }

}
