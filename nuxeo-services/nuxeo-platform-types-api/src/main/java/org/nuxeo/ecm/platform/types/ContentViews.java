/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.types;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Content view descriptor put on document types.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("contentViews")
public class ContentViews implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@append")
    boolean append;

    @XNodeList(value = "contentView", type = String[].class, componentType = String.class)
    String[] contentViews = new String[0];

    public String[] getContentViews() {
        return contentViews;
    }

    public boolean getAppend() {
        return append;
    }

}
