/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.vocabularies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.types.TypeFamily;

public final class VocabularyManager implements Registrable {

    private static final Log log = LogFactory.getLog(VocabularyManager.class);

    private final Map<String, Vocabulary> vocabularies = new HashMap<String, Vocabulary>();

    public List<VocabularyItem> getItems(String name) {
        VocabularyType vocabularyType = (VocabularyType) Manager.getTypeRegistry().lookup(
                TypeFamily.VOCABULARY, name);
        if (vocabularyType == null) {
            return null;
        }
        final String className = vocabularyType.getClassName();
        if (className == null) {
            log.error("Vocabulary class name not set for: " + name);
            return null;
        }
        Vocabulary vocabulary = getInstance(className);
        if (vocabulary == null) {
            log.error("Vocabulary class not found: " + className);
            return null;
        }
        return vocabulary.getItems();
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

    public void clear() {
        // TODO Auto-generated method stub
    }

}
