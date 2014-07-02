/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.DefaultFulltextParser;
import org.nuxeo.ecm.core.storage.FulltextExtractorWork;
import org.nuxeo.ecm.core.storage.FulltextParser;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work task that does fulltext extraction from the blobs of the given document.
 * <p>
 * The extracted fulltext is then passed to the single-threaded
 * {@link FulltextUpdaterWork}.
 *
 * @since 5.7
 */
public class SQLFulltextExtractorWork extends FulltextExtractorWork {

    private static final long serialVersionUID = 1L;

    public SQLFulltextExtractorWork(String repositoryName, String docId) {
        super(repositoryName, docId, repositoryName + ':' + docId
                + ":sqlFulltextExtractor", true);
    }

    private static final Log log = LogFactory.getLog(SQLFulltextExtractorWork.class);

    @Override
    public void initFulltextConfigurationAndParser() {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        fulltextConfiguration = sqlRepositoryService.getFulltextConfiguration(repositoryName);
        Class<? extends FulltextParser> fulltextParserClass = sqlRepositoryService.getFulltextParserClass(repositoryName);
        fulltextParser = new DefaultFulltextParser();
        if (fulltextParserClass != null) {
            try {
                fulltextParser = fulltextParserClass.newInstance();
            } catch (InstantiationException e) {
                log.error(
                        "Failed to instantiate "
                                + fulltextParserClass.getCanonicalName(), e);
            } catch (IllegalAccessException e) {
                log.error(e);
            }
        }
    }

}
