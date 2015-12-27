/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.dom.client.Node;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
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
