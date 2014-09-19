/*
 * (C) Copyright 2006-2014 Nuxeo SA <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 */

package org.nuxeo.theme.vocabularies;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.types.TypeFamily;

public final class VocabularyManager implements Registrable {

    private static final Log log = LogFactory.getLog(VocabularyManager.class);

    private final Map<String, Vocabulary> vocabularies = new HashMap<>();

    public List<VocabularyItem> getItems(String name) {
        VocabularyType vocabularyType = (VocabularyType) Manager.getTypeRegistry().lookup(
                TypeFamily.VOCABULARY, name);
        if (vocabularyType == null) {
            return null;
        }
        final String path = vocabularyType.getPath();
        final String className = vocabularyType.getClassName();

        if (path == null && className == null) {
            log.error("Must specify a class name or a path for vocabulary: "
                    + name);
            return null;
        }
        if (path != null && className != null) {
            log.error("Cannot specify both a class name and a path for vocabulary: "
                    + name);
            return null;
        }

        if (className != null) {
            Vocabulary vocabulary = getInstance(className);
            if (vocabulary == null) {
                log.error("Vocabulary class not found: " + className);
                return null;
            }
            return vocabulary.getItems();
        }

        if (path != null) {
            if (!path.endsWith(".csv")) {
                log.error("Only .csv vocabularies are supported: " + path);
                return null;
            }
            final List<VocabularyItem> items = new ArrayList<>();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(
                    path)) {
                if (is == null) {
                    log.error("Vocabulary file not found: " + path);
                    return null;
                }
                try (CSVParser reader = new CSVParser(new InputStreamReader(is,
                        Charsets.UTF_8), CSVFormat.DEFAULT)) {
                    for (CSVRecord record : reader) {
                        final String value = record.get(0);
                        String label = value;
                        if (record.size() >= 2) {
                            label = record.get(1);
                        }
                        items.add(new VocabularyItem(value, label));
                    }
                }
            } catch (IOException e) {
                log.error("Could not read vocabulary file: " + path, e);
            }
            return items;
        }
        return null;
    }

    private Vocabulary getInstance(String className) {
        Vocabulary vocabulary = vocabularies.get(className);
        if (vocabulary == null) {
            try {
                vocabulary = (Vocabulary) Class.forName(className).newInstance();
            } catch (InstantiationException e) {
                log.error("Could not instantiate vocabulary: " + className);
            } catch (IllegalAccessException e) {
                log.error("Could not instantiate vocabulary: " + className);
            } catch (ClassNotFoundException e) {
                log.error("Could not instantiate vocabulary: " + className);
            }
        }
        if (vocabulary != null) {
            vocabularies.put(className, vocabulary);
        }
        return vocabulary;
    }

    @Override
    public void clear() {
        // FIXME: should call vocabularies.clear() ?
    }

}
