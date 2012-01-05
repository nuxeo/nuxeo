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
package org.nuxeo.ecm.platform.diff.model;

import java.io.Serializable;

/**
 * <p>
 * Representation of a property (or field) diff.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public abstract class PropertyDiff implements Serializable {

    private static final long serialVersionUID = -8458912212588012911L;

    /**
     * Gets the property type.
     * 
     * @return the property type
     */
    public abstract PropertyType getPropertyType();

    /**
     * Checks if is left side empty.
     * 
     * @return true, if is left side empty
     */
    public abstract boolean isLeftSideEmpty();

    /**
     * Checks if is right side empty.
     * 
     * @return true, if is right side empty
     */
    public abstract boolean isRightSideEmpty();

    /**
     * Checks if is empty.
     * 
     * @param leftSide the left side
     * @return true, if is empty
     */
    public boolean isEmpty(boolean leftSide) {
        return leftSide ? isLeftSideEmpty() : isRightSideEmpty();
    }

}
