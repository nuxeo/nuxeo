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

package org.nuxeo.theme.test;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.vocabularies.Vocabulary;
import org.nuxeo.theme.vocabularies.VocabularyItem;

public final class DummyVocabulary implements Vocabulary {

    public List<VocabularyItem> getItems() {
        List<VocabularyItem> items = new ArrayList<VocabularyItem>();
        items.add(new VocabularyItem("value1", "label1"));
        items.add(new VocabularyItem("value2", "label2"));
        return items;
    }

}
