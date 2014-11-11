/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.test.vocabularies;

import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.vocabularies.VocabularyItem;
import org.nuxeo.theme.vocabularies.VocabularyManager;

public class TestVocabularies extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "vocabulary-config.xml");
    }

    public void testClassVocabulary() {
        VocabularyManager vocabularyManager = Manager.getVocabularyManager();
        List<VocabularyItem> items = vocabularyManager.getItems("test vocabulary as class");
        assertEquals(2, items.size());
        assertEquals("value1", items.get(0).getValue());
        assertEquals("label1", items.get(0).getLabel());
        assertEquals("value2", items.get(1).getValue());
        assertEquals("label2", items.get(1).getLabel());
    }

    public void testResourceVocabulary() {
        VocabularyManager vocabularyManager = Manager.getVocabularyManager();
        List<VocabularyItem> items = vocabularyManager.getItems("test vocabulary as csv resource");
        assertEquals(3, items.size());
        assertEquals("value1", items.get(0).getValue());
        assertEquals("label1", items.get(0).getLabel());
        assertEquals("value2", items.get(1).getValue());
        assertEquals("label2", items.get(1).getLabel());
        assertEquals("value3", items.get(2).getValue());
        assertEquals("value3", items.get(2).getLabel());
    }

}
