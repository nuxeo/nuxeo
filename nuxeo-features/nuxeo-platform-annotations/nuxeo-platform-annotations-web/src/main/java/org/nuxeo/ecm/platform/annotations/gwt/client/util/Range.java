/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Node;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class Range {

    private String selectedText;
    private Node startContainer;
    private int startOffset;
    private Node endContainer;
    private int endOffset;

    public Range(String selectedText, Node startContainer, int startOffset, Node endContainer, int endOfsset) {
        this.selectedText = selectedText;
        this.startContainer = startContainer;
        this.startOffset = startOffset;
        this.endContainer = endContainer;
        this.endOffset = endOfsset;
    }

    public String getSelectedText() {
        return selectedText;
    }

    public Node getStartContainer() {
        return startContainer;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public Node getEndContainer() {
        return endContainer;
    }

    public int getEndOffset() {
        return endOffset;
    }

}
