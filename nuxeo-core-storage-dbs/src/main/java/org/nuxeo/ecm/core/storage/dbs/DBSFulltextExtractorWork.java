/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage.dbs;

import org.nuxeo.ecm.core.storage.DefaultFulltextParser;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextExtractorWork;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork;

/**
 * Work task that does fulltext extraction from the blobs of the given document.
 * <p>
 * The extracted fulltext is then passed to the single-threaded
 * {@link FulltextUpdaterWork}.
 *
 * @since 5.7
 */
public class DBSFulltextExtractorWork extends FulltextExtractorWork {

    private static final long serialVersionUID = 1L;

    public DBSFulltextExtractorWork(String repositoryName, String docId) {
        super(repositoryName, docId, repositoryName + ':' + docId
                + ":dbsFulltextExtractor", true);
    }

    @Override
    public void initFulltextConfigurationAndParser() {
        fulltextConfiguration = new FulltextConfiguration();
        fulltextConfiguration.indexNames.add("default");
        fulltextConfiguration.indexesAllBinary.add("default");
        fulltextParser = new DefaultFulltextParser();
    }

}
