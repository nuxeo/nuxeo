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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotationPopupEventListener;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NuxeoDecoratorVisitor implements DecoratorVisitor {

    protected boolean decorating;

    protected final Node startNode;

    protected final Node endNode;

    protected boolean started;

    protected int startOffset;

    protected int endOffset;

    protected final Annotation annotation;

    protected final AnnotationController controller;

    protected boolean endNodeFound = false;

    protected boolean endNodeBeforeStartNode = false;

    protected Node currentNode;

    public NuxeoDecoratorVisitor(Annotation annotation, AnnotationController controller) {
        this.annotation = annotation;
        this.controller = controller;
        XPathUtil xpathUtil = new XPathUtil();
        Document document = Document.get();
        startNode = xpathUtil.getNode(annotation.getStartContainer().getXpath(), document).get(0);
        startOffset = annotation.getStartContainer().getOffset();
        endNode = xpathUtil.getNode(annotation.getEndContainer().getXpath(), document).get(0);
        endOffset = annotation.getEndContainer().getOffset();
        Log.debug("Decorator -- start node: " + startNode + ";text: "
                + ((com.google.gwt.dom.client.Element) startNode).getInnerHTML() + ";parent html: "
                + ((Element) startNode.getParentNode()).getInnerHTML());
        Log.debug("Decorator -- end node: " + endNode + ";text: "
                + ((com.google.gwt.dom.client.Element) endNode).getInnerHTML() + ";parent html: "
                + ((Element) endNode.getParentNode()).getInnerHTML());
        Log.debug("Decorator -- start offset: " + startOffset + "; end offset: " + endOffset);
    }

    public void process(Node node) {
        currentNode = node;
        checkEndNodeBeforeStartNode();
        shouldStartProcess();
        processNodeIfStarted();
    }

    protected void checkEndNodeBeforeStartNode() {
        if (started || startNode.equals(endNode)) {
            return; // start node already found
        }
        if (currentNode.equals(endNode)) {
            Log.debug("Decorator -- EndNodeBeforeStartNode found.");
            endNodeBeforeStartNode = true;
        }
    }

    protected void shouldStartProcess() {
        if (currentNode.equals(startNode) && !started) {
            Log.debug("Decorator -- start node found: " + currentNode + ";text: " + currentNode.getNodeValue());
            Log.debug("Decorator -- parent html: " + ((Element) currentNode.getParentNode()).getInnerHTML());
            started = true;
            if (startNode.equals(endNode)) {
                endNodeFound = true;
            }
        }
    }

    protected void processNodeIfStarted() {
        if (started) {
            processNode();
        }
    }

    protected void processNode() {
        if (!decorating) {
            processToFirstNode();
        } else {
            decorateNode();
        }
    }

    protected void processToFirstNode() {
        Log.debug("Decorator -- processToFirstNode: " + currentNode.getNodeName());
        if (!(currentNode.getNodeType() == Node.TEXT_NODE)) {
            return;
        }
        Text text = (Text) currentNode;
        String data = text.getData();
        Log.debug("Decorator -- text data before: " + data);
        data = Utils.removeWhitespaces(data, currentNode);
        Log.debug("Decorator -- text data after: " + data);
        if (data.length() < startOffset) {
            startOffset -= data.length();
            if (startNode.equals(endNode)) {
                endOffset -= data.length();
            }
            return;
        }
        decorating = true;

        String notInData = data.substring(0, startOffset);
        decorateText(data.substring(startOffset));
        text.setData(notInData);
    }

    protected void decorateText(String textToDecorate) {
        checkEndNodeFound();

        String afterText = getAfterText();
        Log.debug("Decorator -- afterText: " + afterText);
        if (afterText.length() > 0) {
            textToDecorate = textToDecorate.substring(0, textToDecorate.length() - afterText.length());
        }

        if (currentNode.getParentNode().getNodeName().equalsIgnoreCase("tr")) {
            // don't add nodes to tr
            return;
        }

        com.google.gwt.dom.client.Element spanElement = decorateTextWithSpan(textToDecorate);
        if (spanElement == null) {
            if (afterText.length() > 0) {
                Document document = currentNode.getOwnerDocument();
                Node parent = currentNode.getParentNode();
                insertBefore(parent, currentNode, document.createTextNode(afterText));
            }
        } else {
            Log.debug("Decorator -- span element: " + spanElement.getInnerHTML());
            if (afterText.length() > 0) {
                Document document = currentNode.getOwnerDocument();
                Node parent = currentNode.getParentNode();
                insertBefore(parent, spanElement.getNextSibling(), document.createTextNode(afterText));
            }
        }
    }

    protected void checkEndNodeFound() {
        Log.debug("Decorator -- endNode: " + endNode);
        Log.debug("Decorator -- currentNode: " + currentNode);
        Log.debug("Decorator -- endNode == currentNode?: " + currentNode.equals(endNode));
        if (currentNode.equals(endNode)) {
            endNodeFound = true;
            Log.debug("Decorator -- end node found: " + currentNode + ";text: " + currentNode.getNodeValue());
            Log.debug("Decorator -- parent html: " + ((Element) currentNode.getParentNode()).getInnerHTML());
        }
    }

    protected String getAfterText() {
        Text text = (Text) currentNode;
        String data = text.getData();
        Log.debug("Decorator -- text data before: " + data);
        data = Utils.removeWhitespaces(data, currentNode);
        Log.debug("Decorator -- text data after: " + data);

        String afterText = "";
        if (endNodeFound) {
            if (data.length() > endOffset) {
                afterText = data.substring(endOffset);
                data = data.substring(0, endOffset);
            }
            endOffset -= data.length();
        }
        return afterText;
    }

    protected com.google.gwt.dom.client.Element decorateTextWithSpan(String data) {
        if (data.trim().length() == 0) {
            // don't add span to empty text
            return null;
        }

        Document document = currentNode.getOwnerDocument();
        SpanElement spanElement = getSpanElement(document);
        spanElement.setInnerText(data);
        Node parent = currentNode.getParentNode();
        String className = AnnotationConstant.IGNORED_ELEMENT + " " + controller.getDecorateClassName() + " "
                + AnnotationConstant.DECORATE_CLASS_NAME + annotation.getId();
        if (parent.getNodeName().equalsIgnoreCase("span")) {
            String parentClassName = ((SpanElement) parent.cast()).getClassName();
            if (parentClassName.indexOf(controller.getDecorateClassName()) != -1) {
                className = parentClassName + " " + AnnotationConstant.DECORATE_CLASS_NAME + annotation.getId();
            }
        }
        spanElement.setClassName(className);
        insertBefore(parent, currentNode.getNextSibling(), spanElement);
        return spanElement;
    }

    protected void decorateNode() {
        if (endNodeBeforeStartNode
                && (endNode.equals(currentNode.getPreviousSibling()) || endNode.equals(currentNode.getParentNode()))) {
            endNodeFound = true;
            endOffset = 0;
            return;
        }
        if (!(currentNode.getNodeType() == Node.TEXT_NODE)) {
            if (endNode.equals(currentNode.getPreviousSibling())) {
                endNodeFound = true;
                endOffset = 0;
            } else if (endNode.equals(currentNode)) {
                endNodeFound = true;
            }
            return;
        }
        Text text = (Text) currentNode;
        String data = text.getData();
        data = Utils.removeWhitespaces(data, currentNode);
        decorateText(data);
        currentNode.getParentNode().removeChild(currentNode);
    }

    protected SpanElement getSpanElement(Document document) {
        SpanElement spanElement = document.createSpanElement();
        DOM.sinkEvents((Element) spanElement.cast(), Event.ONMOUSEOVER | Event.ONMOUSEOUT);
        DOM.setEventListener((Element) spanElement.cast(),
                AnnotationPopupEventListener.getAnnotationPopupEventListener(annotation, controller));
        return spanElement;
    }

    protected void insertBefore(Node parent, Node child, Node newChild) {
        if (child == null) {
            parent.appendChild(newChild);
        } else {
            parent.insertBefore(newChild, child);
        }
    }

    public boolean doBreak() {
        return endNodeFound && endOffset <= 0;
    }

}
