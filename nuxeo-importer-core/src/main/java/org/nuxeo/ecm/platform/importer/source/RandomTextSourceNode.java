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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.platform.importer.random.DictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.HunspellDictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;


/**
 * Random {@link SourceNode} to be used for load testing
 *
 * @author Thierry Delprat
 */
public class RandomTextSourceNode implements SourceNode {

    private static final Log log = LogFactory.getLog(RandomTextSourceNode.class);

    protected static RandomTextGenerator gen;

    protected static int maxNode = 10000;

    /**
     * Used in {@link #getMaxChildren()} and {@link #getMaxFolderish()}.
     */
    protected static boolean nonUniformRepartition = false;

    public static final int MAX_DEPTH = 8;

    public static final int DEFAULT_NB_DATA_NODES_PER_FOLDER = 100;

    /**
     * Used to generate a big number of children nodes when {@link #nonUniformRepartition} is {@code true}.
     */
    public static final int BIG_NB_NODES_FACTOR = 50;

    /**
     * Used to generate a small number of children nodes when {@link #nonUniformRepartition} is {@code true}.
     */
    public static final int SMALL_NB_BODES_DIVIDER = DEFAULT_NB_DATA_NODES_PER_FOLDER;

    protected static int minGlobalFolders = 0;

    protected static int minFoldersPerNode = 0;

    protected static AtomicInteger nbNodes;

    protected static AtomicInteger nbFolders;

    protected static AtomicInteger nbVisitedFolders;

    protected static AtomicLong size;

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected String name;

    protected boolean folderish;

    protected int level = 0;

    protected int idx = 0;

    protected static Integer blobSizeInKB;

    protected List<SourceNode> cachedChildren = null;

    protected static final boolean CACHE_CHILDREN = false;

    protected boolean onlyText = true;

    protected boolean withProperties = false;

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

    public RandomTextSourceNode(boolean folderish, int level, int idx, boolean onlyText, boolean withProperties) {
        this.folderish = folderish;
        this.level = level;
        this.idx = idx;
        this.onlyText = onlyText;
        this.withProperties = withProperties;
    }

    public RandomTextSourceNode(boolean folderish, int level, int idx, boolean onlyText) {
        this(folderish, level, idx, onlyText, false);
    }

    public static RandomTextSourceNode init(int maxSize) {
        return init(maxSize, null, true);
    }

    public static RandomTextSourceNode init(int maxSize, Integer blobSizeInKB, boolean onlyText) {
        return init(maxSize, blobSizeInKB, onlyText, false, false, null);
    }

    public static RandomTextSourceNode init(int maxSize, Integer blobSizeInKB, boolean onlyText, boolean nonUniform,
                                            boolean withProperties, String lang) {
        return init(maxSize, blobSizeInKB, onlyText, new HunspellDictionaryHolder(lang), nonUniform,
                withProperties);
    }

    public static RandomTextSourceNode init(int maxSize, Integer blobSizeInKB, boolean onlyText,
            DictionaryHolder dictionaryHolder, boolean nonUniform, boolean withProperties) {
        gen = new RandomTextGenerator(dictionaryHolder);
        gen.prefilCache();
        maxNode = maxSize;
        nbNodes = new AtomicInteger(0);
        nbFolders = new AtomicInteger(1);
        nbVisitedFolders = new AtomicInteger(0);
        size = new AtomicLong(0);
        RandomTextSourceNode.blobSizeInKB = blobSizeInKB;
        minGlobalFolders = maxNode / DEFAULT_NB_DATA_NODES_PER_FOLDER;
        minFoldersPerNode = 1 + (int) Math.pow(minGlobalFolders, (1.0 / MAX_DEPTH));
        nonUniformRepartition = nonUniform;
        return new RandomTextSourceNode(true, 0, 0, onlyText, withProperties);
    }

    protected String getBlobMimeType() {
        if (onlyText) {
            return "text/plain";
        } else {
            return "text/partial";
        }
    }

    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    @Override
    public BlobHolder getBlobHolder() {
        String content = null;
        if (folderish) {
            if (withProperties) {
                return new SimpleBlobHolderWithProperties((Blob) null, getRandomProperties(content));
            }
            return null;
        }
        if (blobSizeInKB == null) {
            content = gen.getRandomText();
        } else {
            content = gen.getRandomText(blobSizeInKB);
        }
        size.addAndGet(content.length());
        Blob blob = Blobs.createBlob(content, getBlobMimeType(), null, getName() + ".txt");
        if (withProperties) {
            return new SimpleBlobHolderWithProperties(blob, getRandomProperties(content));
        }
        return new SimpleBlobHolder(blob);
    }

    protected Map<String, Serializable> getRandomProperties(String content) {
        Map<String, Serializable> ret = new HashMap<>();
        ret.put("dc:title", capitalize(getName()));
        if (RANDOM.nextInt(10) == 1) {
            String description;
            if (content != null && ! content.isEmpty()) {
                description = content.substring(0, content.indexOf(' ', 40));
            } else {
                description = gen.getRandomTitle(RANDOM.nextInt(5)+1);
            }
            ret.put("dc:description", capitalize(description));
        }
        ret.put("dc:nature", getGaussian(DC_NATURE));
        ret.put("dc:subjects", (Serializable) Arrays.asList(getGaussian(DC_SUBJECTS)));
        ret.put("dc:rights", getGaussian(DC_RIGHTS));
        ret.put("dc:language", getGaussian(DC_LANGUAGE));
        ret.put("dc:coverage", getGaussian(DC_COVERAGE));
        ret.put("dc:source", getGaussian(DC_SOURCE));
        // validation contraint violation
        // ret.put("dc:creator", String.format("user%03d", hazard.nextInt(500)));
        return ret;
    }

    protected String getGaussian(String[] words) {
        double g = Math.abs(RANDOM.nextGaussian() / 4);
        g = Math.min(g, 1);
        int i = (int) Math.floor(g * (words.length - 1));
        return words[ i ];
    }

    protected int getMidRandom(int target) {
        return 1 + (target / 2) + RANDOM.nextInt(target);
    }

    /**
     * Allows to get a non uniform distribution of the number of nodes per folder. Returns:
     * <ul>
     * <li>A small number of nodes 10% of the time, see {@link #SMALL_NB_BODES_DIVIDER}.</li>
     * <li>A big number of nodes 10% of the time, see {@link #BIG_NB_NODES_FACTOR}.</li>
     * <li>A random variation of the target number of nodes 80% of the time.</li>
     * </ul>
     */
    protected int getNonUniform(int target, boolean folderish) {
        int res;
        int remainder = nbVisitedFolders.get() % 10;
        if (remainder == 8) {
            res = 1 + target / SMALL_NB_BODES_DIVIDER;
            if (log.isDebugEnabled()) {
                String nodeStr;
                if (folderish) {
                    nodeStr = "folderish";
                } else {
                    nodeStr = "data";
                }
                log.debug(String.format("### Small number of %s nodes: %d", nodeStr, res));
            }
        } else if (remainder == 9) {
            int factor;
            // Big number of folderish nodes is 10 times smaller than the big number of data nodes
            if (folderish) {
                factor = BIG_NB_NODES_FACTOR / 10;
            } else {
                factor = BIG_NB_NODES_FACTOR;
            }
            res = 1 + target * factor;
            if (log.isDebugEnabled()) {
                String nodeStr;
                if (folderish) {
                    nodeStr = "folderish";
                } else {
                    nodeStr = "data";
                }
                log.debug(String.format("### Big number of %s nodes: %d", nodeStr, res));
            }
        } else {
            res = getMidRandom(target);
        }
        return res;
    }

    protected int getMaxChildren() {
        if (maxNode < nbNodes.get()) {
            return 0;
        }
        int targetRemainingFolders = minGlobalFolders - nbFolders.get();
        if (targetRemainingFolders <= 0) {
            return DEFAULT_NB_DATA_NODES_PER_FOLDER + 1;
        }
        int target = ((maxNode - nbNodes.get()) / targetRemainingFolders);
        if (target <= 0) {
            return 0;
        }
        if (nonUniformRepartition) {
            return getNonUniform(target, false);
        } else {
            return getMidRandom(target);
        }
    }

    protected int getMaxFolderish() {
        if (maxNode <= nbNodes.get()) {
            return 0;
        }
        if (nonUniformRepartition) {
            return getNonUniform(minFoldersPerNode, true);
        } else {
            return getMidRandom(minFoldersPerNode);
        }
    }

    @Override
    public List<SourceNode> getChildren() {

        if (!folderish) {
            return null;
        }

        if (cachedChildren != null) {
            return cachedChildren;
        }

        List<SourceNode> children = new ArrayList<SourceNode>();
        if (nbNodes.get() > maxNode) {
            return children;
        }

        int nbChildren = getMaxChildren();
        for (int i = 0; i < nbChildren; i++) {
            children.add(new RandomTextSourceNode(false, level, i, onlyText, withProperties));
        }
        nbNodes.addAndGet(nbChildren);
        if (log.isDebugEnabled()) {
            String nodeStr;
            if (nbChildren > 1) {
                nodeStr = "nodes";
            } else {
                nodeStr = "node";
            }
            log.debug(String.format("Added %s data %s to %s; data node total count = %s", nbChildren, nodeStr,
                    getName(), nbNodes));
        }

        if (level < MAX_DEPTH) {
            // In the case of a non uniform repartition, don't add folderish nodes if there are no data nodes to not
            // overload the tree with folderish nodes that would probably be empty
            if (!nonUniformRepartition || nbChildren > 0) {
                int nbFolderish = getMaxFolderish();
                for (int i = 0; i < nbFolderish; i++) {
                    children.add(new RandomTextSourceNode(true, level + 1, i, onlyText, withProperties));
                }
                nbFolders.addAndGet(nbFolderish);
                if (log.isDebugEnabled()) {
                    String nodeStr;
                    if (nbFolderish > 1) {
                        nodeStr = "nodes";
                    } else {
                        nodeStr = "node";
                    }
                    log.debug(String.format("Added %s folderish %s to %s; folderish node total count = %s",
                            nbFolderish, nodeStr, getName(), nbFolders));
                }
            }
        }
        if (CACHE_CHILDREN) {
            cachedChildren = children;
        }

        nbVisitedFolders.incrementAndGet();
        if (log.isDebugEnabled()) {
            String folderStr;
            if (nbVisitedFolders.get() > 1) {
                folderStr = "folders";
            } else {
                folderStr = "folder";
            }
            log.debug(String.format("Visited %s %s", nbVisitedFolders, folderStr));
        }

        return children;
    }

    @Override
    public String getName() {
        if (name == null) {
            if (withProperties) {
                name = gen.getRandomTitle(RANDOM.nextInt(3)+1);
            }
            else {
                if (folderish) {
                    name = "folder";
                } else {
                    name = "file";
                }
                if (level == 0 && folderish) {
                    name = name + "-" + (System.currentTimeMillis() % 10000) + RANDOM.nextInt(100);
                } else {
                    name = name + "-" + level + "-" + idx;
                }
            }
        }
        return name;
    }

    @Override
    public boolean isFolderish() {
        return folderish;
    }

    public static Integer getNbNodes() {
        return nbNodes.get();
    }

    public static Long getSize() {
        return size.get();
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String getSourcePath() {
        return null;
    }
}
