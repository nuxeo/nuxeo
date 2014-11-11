/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.syndication.vocabularies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public final class HierarchicalVocabulary {

    public static final Comparator<HierarchicalVocabulary> ORDER_BY_ID = new Comparator<HierarchicalVocabulary>() {

        public int compare(HierarchicalVocabulary o1,
                HierarchicalVocabulary o2) {
            return o1.getVocabulary().getId().compareToIgnoreCase(
                    o2.getVocabulary().getId());
        }

    };

    private final SimpleVocabulary simpleVocabulary;

    private final HierarchicalVocabulary parent;

    private final List<HierarchicalVocabulary> children = new ArrayList<HierarchicalVocabulary>();

    private boolean sorted = false;

    public HierarchicalVocabulary(HierarchicalVocabulary parent, SimpleVocabulary vocabulary) {
        simpleVocabulary = vocabulary;
        this.parent = parent;
    }

    public void addChild(HierarchicalVocabulary voca) {
        children.add(voca);
    }

    public void addChild(SimpleVocabulary voca) {
        addChild(new HierarchicalVocabulary(this, voca));
    }

    public void removeChild(HierarchicalVocabulary voca) {
        children.remove(voca);
    }

    public HierarchicalVocabulary getParent() {
        return parent;
    }

    public SimpleVocabulary getVocabulary() {
        return simpleVocabulary;
    }

    public List<HierarchicalVocabulary> getChildren() {
        if (!sorted) {
            Collections.sort(children, ORDER_BY_ID);
            sorted = true;
        }
        return Collections.unmodifiableList(children);
    }

    public boolean hasParent() {
        return null != parent;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

}
