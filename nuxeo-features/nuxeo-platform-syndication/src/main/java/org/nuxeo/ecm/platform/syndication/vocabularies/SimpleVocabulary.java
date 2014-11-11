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

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public final class SimpleVocabulary {

    private final String id;

    private final String label;

    private final String translatedLabel;

    private  String vocabularyName ="" ;

    public SimpleVocabulary(final String id, final String label, final String translatedLabel) {
        this.id = id;
        this.label = label;
        this.translatedLabel = translatedLabel;
    }

    public SimpleVocabulary(final String id, final String label, final String translatedLabel, String vocabularyName ) {
        this.id = id;
        this.label = label;
        this.translatedLabel = translatedLabel;
        this.vocabularyName = vocabularyName;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getTranslatedLabel() {
        return translatedLabel;
    }

    public String getvocabularyName() {
        return vocabularyName;
    }

}
