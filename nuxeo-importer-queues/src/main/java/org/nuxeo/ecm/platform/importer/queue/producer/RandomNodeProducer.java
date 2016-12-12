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
package org.nuxeo.ecm.platform.importer.queue.producer;

import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.random.HunspellDictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;
import org.nuxeo.ecm.platform.importer.source.ImmutableNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * @since 9.1
 */
public class RandomNodeProducer extends AbstractProducer<ImmutableNode> {

    private final int nbDocuments;
    private final int nbConsumers;
    private boolean countFolderAsDocument = true;
    private int maxFoldersPerFolder = 50;
    private int maxDocumentsPerFolder = 500;
    private String lang = "en_US";

    private int blobSizeKB = 0;
    private boolean blobOnlyText = false;
    private int documentCount = 0;
    private int folderCount = 0;
    private final Random rand;
    private static RandomTextGenerator gen;

    static protected final String[] DC_NATURE = {"article", "acknowledgement", "assessment", "application", "order",
            "contract", "quotation", "fax", "worksheet", "letter", "memo", "note", "notification", "procedure",
            "report", "internshipReport", "pressReview"};

    static protected final String[] DC_SUBJECTS = {"art/architecture", "art/comics", "art/cinema", "art/culture", "art/danse",
            "art/music", "sciences/astronomy", "sciences/biology", "sciences/chemistry", "sciences/math",
            "sciences/physic", "society/ecology", "daily life/gastronomy", "daily life/gardening", "daily life/sport",
            "technology/it"};

    static protected final String[] DC_RIGHTS = {"OpenContentL", "CC-BY-NC", "CC-BY-ND", "FreeArt", "ODbi", "GNUGPL",
            "FreeBSD", "CC0"};

    static protected final String[] DC_LANGUAGE = {"IT", "DE", "FR", "US", "EN"};

    static protected final String[] DC_SOURCE = {"internal", "external", "unknown"};

    static protected final String[] DC_COVERAGE = {"europe/France", "europe/Germany", "europe/Italy", "europe/Spain",
            "oceania/Tonga", "africa/Mali", "asia/Japan", "north-america/United_States_of_America"};


    public RandomNodeProducer(ImporterLogger log, int nbDocuments, int nbConsumers) {
        super(log);
        this.nbDocuments = nbDocuments;
        this.nbConsumers = nbConsumers;
        rand = new Random(System.currentTimeMillis());
        if (gen == null) {
            gen = new RandomTextGenerator(new HunspellDictionaryHolder(lang));
            gen.prefilCache();
        }
    }

    public RandomNodeProducer setMaxFoldersPerFolder(int max) {
        maxFoldersPerFolder = max;
        return this;
    }

    public RandomNodeProducer setMaxDocumentsPerFolder(int max) {
        maxDocumentsPerFolder = max;
        return this;
    }

    public RandomNodeProducer countFolderAsDocument(boolean value) {
        countFolderAsDocument = value;
        return this;
    }

    public RandomNodeProducer setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public RandomNodeProducer withBlob(int sizeKB, boolean onlyText) {
        this.blobSizeKB = sizeKB;
        this.blobOnlyText = onlyText;
        return this;
    }

    @Override
    public void run() {
        try {
            started = true;
            int nbDocumentsPerConsumer = (int) Math.ceil((double) nbDocuments / nbConsumers);
            for (int i = 0; i < nbConsumers; i++) {
                createDocs(i, nbDocumentsPerConsumer);
            }
            completed = true;
        } catch (Exception e) {
            log.error("Error during sourceNode processing", e);
            error = e;
        }
    }

    public void createDocs(int consumerId, int nbDocs) throws InterruptedException {
        int nbCreated = 0;
        int level = 0;
        List<String> parents = new ArrayList<>();
        parents.add("");
        do {
            List<String> children = new ArrayList<>();
            for (String folder : parents) {
                for (int i = 0; i < 1 + rand.nextInt(maxFoldersPerFolder - 1); i++) {
                    children.add(createFolder(consumerId, folder));
                    if (countFolderAsDocument) {
                        nbCreated++;
                        if (nbCreated >= nbDocs || documentCount >= nbDocuments) {
                            return;
                        }
                    }
                }
                for (int i = 0; i < rand.nextInt(maxDocumentsPerFolder); i++) {
                    createDocument(consumerId, folder);
                    nbCreated++;
                    if (nbCreated >= nbDocs || documentCount >= nbDocuments) {
                        return;
                    }
                }
            }
            parents = children;
            level++;
            // System.out.println("level " + level + ", children: " + parents.size());
        } while (nbCreated < nbDocs && documentCount < nbDocuments);
    }

    private String createFolder(int consumerId, String parentPath) throws InterruptedException {
        ImmutableNode node = getRandomNode(consumerId, "Folder", parentPath);
        String ret = node.getPath();
        // log.debug("F:" + node.getPath());
        dispatch(node);
        if (countFolderAsDocument) {
            folderCount = documentCount++;
        } else {
            folderCount++;
        }
        // System.out.println(">>> F:" + node.getPath());
        return node.getPath();
    }

    @Override
    public int getTargetQueue(ImmutableNode node, int nbQueues) {
        return node.getPartition() % nbQueues;
    }

    private String createDocument(int consumerId, String parentPath) throws InterruptedException {
        ImmutableNode node = getRandomNode(consumerId, "File", parentPath);
        String ret = node.getPath();
        // log.debug("d:" + ret);
        dispatch(node);
        documentCount++;
        // System.out.println(">>> D:" + ret);
        return ret;
    }

    private ImmutableNode getRandomNode(int consumerId, String type, String parentPath) {
        String title = getTitle();
        String name = getName(title);
        HashMap<String, Serializable> props = getRandomProperties(title);
        Blob blob = getRandomBlob();
        return new ImmutableNode.ImmutableNodeBuilder(type, parentPath, name)
                .setPartition(consumerId)
                .setProperties(props)
                .setBlob(blob)
                .build();
    }

    private Blob getRandomBlob() {
        if (blobSizeKB == 0) {
            return null;
        }
        String content = gen.getRandomText(blobSizeKB);
        return Blobs.createBlob(content, getBlobMimeType(), null, getName(getTitle()) + ".txt");
    }

    private String getBlobMimeType() {
        if (blobOnlyText) {
            return "text/plain";
        } else {
            return "text/partial";
        }
    }

    private String getName(String title) {
        return title.replaceAll("\\W+", "-").toLowerCase();
    }

    @NotNull
    private String getTitle() {
        return capitalize(gen.getRandomTitle(rand.nextInt(3) + 1).trim());
        //  return "f" + folderCount;
    }

    private String capitalize(final String line) {
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

}
