/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.diff.model.DiffDisplayListItem;

/**
 * Handles...
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DiffDisplayListItemImpl implements DiffDisplayListItem {

    private static final long serialVersionUID = -3018441537347474675L;

    protected int index;

    protected Serializable value;

    public DiffDisplayListItemImpl(int index, Serializable value) {
        this.index = index;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public Serializable getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (!(other instanceof DiffDisplayListItem)) {
            return false;
        }

        int otherIndex = ((DiffDisplayListItem) other).getIndex();
        Serializable otherValue = ((DiffDisplayListItem) other).getValue();

        return index == otherIndex
                && (value == null && otherValue == null || value.equals(otherValue));
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append(index);
        sb.append(",");
        sb.append(value);
        sb.append("}");

        return sb.toString();
    }

}
