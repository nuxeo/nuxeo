/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotationPopupEventListener;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * @author Alexandre Russel
 */
public class AnnoteaDecoratorVisitor implements DecoratorVisitor {
    private boolean decorating;

    private boolean previousIsCarriageReturnElement;

    private static List<String> carriagesReturnedElements = new ArrayList<String>();
    static {
        carriagesReturnedElements.add("div");
        carriagesReturnedElements.add("br");
        carriagesReturnedElements.add("p");
    }

    private boolean lastCharIsSpace;

    private final Node startNode;

    private boolean started;

    private int offset;

    private int textToAnnotate;

    private final Annotation annotation;

    private final AnnotationController controller;

    public AnnoteaDecoratorVisitor(Node startNode, int annotatedText, int offset, Annotation annotation,
            AnnotationController controller) {
        this.startNode = startNode;
        this.textToAnnotate = annotatedText;
        this.offset = offset;
        this.annotation = annotation;
        this.controller = controller;
    }

    public boolean isLastCharIsSpace() {
        return lastCharIsSpace;
    }

    public void setLastCharIsSpace(boolean lastCharIsSpace) {
        this.lastCharIsSpace = lastCharIsSpace;
    }

    public boolean doBreak() {
        return textToAnnotate == 0;
    }

    public void process(Node node) {
        if (node.equals(startNode)) {
            started = true;
        } else if (started) {
            if (!decorating) {
                processToFirstNode(node);
            } else {
                processNode(node);
            }
            if (carriagesReturnedElements.contains(node.getNodeName().toLowerCase())) {
                previousIsCarriageReturnElement = true;
            } else {
                previousIsCarriageReturnElement = false;
            }
        }
    }

    private void insertBefore(Node parent, Node child, Node newChild) {
        if (child == null) {
            parent.appendChild(newChild);
        } else {
            parent.insertBefore(newChild, child);
        }
    }

    private void processNode(Node node) {
        if (!(node.getNodeType() == Node.TEXT_NODE)) {
            if (node.getNodeName().equalsIgnoreCase("td")) {
                textToAnnotate -= 1;
            }
            return;
        }
        Text text = (Text) node;
        Node parent = text.getParentNode();
        String data = text.getData();
        processDecoratedNode(node, data, parent);
        node.getParentNode().removeChild(node);
    }

    public String[] getSelectedText(String rawText, int length) {
        String text = "";
        for (int x = 0; x <= rawText.length(); x++) {
            text = rawText.substring(0, x);
            text = removeWhiteSpace(text);
            if (text.length() == length) {
                return new String[] { text, rawText.substring(0, x), rawText.substring(x) };
            }
        }
        return new String[] { text, rawText, "" };
    }

    public String removeWhiteSpace(String data) {
        data = data.replaceAll("\\s+", " ");
        boolean startWithSpace = data.startsWith(" ");
        boolean endWithSpace = data.endsWith(" ");
        data = data.trim();
        if (lastCharIsSpace && !startWithSpace && !previousIsCarriageReturnElement) {
            data = " " + data;
        } else if (!lastCharIsSpace && startWithSpace && !previousIsCarriageReturnElement) {
            data = " " + data;
        }
        lastCharIsSpace = endWithSpace;
        return data;
    }

    private SpanElement getSpanElement(Document document) {
        SpanElement spanElement = document.createSpanElement();
        DOM.sinkEvents((Element) spanElement.cast(), Event.ONMOUSEOVER | Event.ONMOUSEOUT);
        DOM.setEventListener((Element) spanElement.cast(),
                AnnotationPopupEventListener.getAnnotationPopupEventListener(annotation, controller));
        spanElement.setClassName(AnnotationConstant.IGNORED_ELEMENT + " " + controller.getDecorateClassName() + " "
                + AnnotationConstant.DECORATE_CLASS_NAME + annotation.getId());
        return spanElement;
    }

    private void processToFirstNode(Node node) {
        if (!(node.getNodeType() == Node.TEXT_NODE)) {
            return;
        }
        Text text = (Text) node;
        String data = text.getData();
        if (data.length() < offset) {
            offset -= data.length();
            return;
        }
        decorating = true;
        Node parent = text.getParentNode();
        if (data.endsWith(" ")) {
            lastCharIsSpace = true;
        }
        String notInData = data.substring(0, offset);
        text.setData(notInData);
        processDecoratedNode(node, data.substring(offset), parent);
    }

    private void processDecoratedNode(Node node, String data, Node parent) {
        String[] selectedText = getSelectedText(data, textToAnnotate);
        if (selectedText[1].trim().length() == 0 && selectedText[2].trim().length() == 0
                && node.getParentNode().getNodeName().equalsIgnoreCase("tr")) {
            // don't add nodes to tr
            textToAnnotate -= selectedText[0].length();
            return;
        }
        Document document = node.getOwnerDocument();
        SpanElement spanElement = getSpanElement(document);
        spanElement.setInnerText(selectedText[1]);
        insertBefore(parent, node.getNextSibling(), spanElement);
        if (selectedText[2].length() > 0) {
            insertBefore(parent, spanElement.getNextSibling(), document.createTextNode(selectedText[2]));
        }
        textToAnnotate -= selectedText[0].length();
    }
}
