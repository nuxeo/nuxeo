/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * This class is used for setting the values of a select box dynamically,
 * i.e. not from a directory.
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
        if(id == null) {
            throw new IllegalArgumentException("id is null");
        }
        if(label == null) {
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
