/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a> This class is used for setting the values of a select
 *         box dynamically, i.e. not from a directory.
 */
public class VocabularyEntry implements Serializable {

    private static final long serialVersionUID = 8242013595942264323L;

    private String id;

    private String label;

    private String parent;

    private Boolean obsolete;

    private Integer ordering;

    public VocabularyEntry(String id, String label) {
        this(id, label, null);
    }

    public VocabularyEntry(String id, String label, String parent) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        if (label == null) {
            throw new IllegalArgumentException("label is null");
        }
        this.id = id;
        this.label = label;
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * @return Returns the obsolete.
     */
    public Boolean getObsolete() {
        return obsolete;
    }

    /**
     * @param obsolete The obsolete to set.
     */
    public void setObsolete(Boolean obsolete) {
        this.obsolete = obsolete;
    }

    /**
     * @return Returns the vocabulary entry order.
     */
    public Integer getOrdering() {
        return ordering;
    }

    /**
     * This method sets the vocabulary entry order.
     *
     * @param ordering The order to set.
     */
    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

}
