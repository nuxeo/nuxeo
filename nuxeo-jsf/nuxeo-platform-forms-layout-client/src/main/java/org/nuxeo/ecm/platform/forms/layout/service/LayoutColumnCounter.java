/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Helper component to count number of columns rendered in a layout context.
 * <p>
 * Useful when rendering listing layouts in a PDF table, where number of
 * columns need to be known in advance.
 *
 * @since 5.4.2
 */
@Name("layoutColumnCounter")
@Scope(ScopeType.EVENT)
public class LayoutColumnCounter {

    protected Integer index;

    public Integer getCurrentIndex() {
        return index;
    }

    public void setCurrentIndex(Integer index) {
        this.index = index;
    }

    public void increment() {
        if (index == null) {
            index = new Integer(0);
        }
        this.index = new Integer(index.intValue() + 1);
    }

    public void decrement() {
        if (index == null) {
            index = new Integer(0);
        }
        this.index = new Integer(index.intValue() - 1);
    }

}
