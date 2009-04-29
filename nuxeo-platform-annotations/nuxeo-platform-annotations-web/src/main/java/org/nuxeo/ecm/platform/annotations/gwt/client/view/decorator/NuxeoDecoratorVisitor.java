/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotationPopupEventListener;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
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

    protected Node currentNode;

    public NuxeoDecoratorVisitor(Annotation annotation,
            AnnotationController controller) {
        this.annotation = annotation;
        this.controller = controller;
        XPathUtil xpathUtil = new XPathUtil();
        Document document = Document.get();
        startNode = xpathUtil.getNode(
                annotation.getStartContainer().getXpath(), document).get(
                0);
        startOffset = annotation.getStartContainer().getOffset();
        endNode = xpathUtil.getNode(annotation.getEndContainer().getXpath(),
                document).get(0);
        endOffset = annotation.getEndContainer().getOffset();
    }

    public void process(Node node) {
        currentNode = node;
        shouldStartProcess();
        processNodeIfStarted();
    }

    protected void shouldStartProcess() {
        if (currentNode.equals(startNode) && !started) {
            started = true;
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
        if (!(currentNode.getNodeType() == Node.TEXT_NODE)) {
            return;
        }
        Text text = (Text) currentNode;
        String data = text.getData();
        if (data.length() < startOffset) {
            startOffset -= data.length();
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
        if (afterText.length() > 0) {
            textToDecorate = textToDecorate.substring(0,
                    textToDecorate.length() - afterText.length());
        }

        SpanElement spanElement = decorateTextWithSpan(textToDecorate);
        if (afterText.length() > 0) {
            Document document = currentNode.getOwnerDocument();
            Node parent = currentNode.getParentNode();
            insertBefore(parent, spanElement.getNextSibling(),
                    document.createTextNode(afterText));
        }
    }

    protected void checkEndNodeFound() {
        if (currentNode.equals(endNode)) {
            endNodeFound = true;
        }
    }

    protected String getAfterText() {
        Text text = (Text) currentNode;
        String data = text.getData();

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

    protected SpanElement decorateTextWithSpan(String data) {
        if (currentNode.getParentNode().getNodeName().equalsIgnoreCase("tr")) {
            // don't add nodes to tr
            return null;
        }

        Document document = currentNode.getOwnerDocument();
        SpanElement spanElement = getSpanElement(document);
        spanElement.setInnerText(data);
        Node parent = currentNode.getParentNode();
        String className = AnnotationConstant.IGNORED_ELEMENT + " "
                + controller.getDecorateClassName() + " "
                + AnnotationConstant.DECORATE_CLASS_NAME + annotation.getId();
        if (parent.getNodeName().equalsIgnoreCase("span")) {
            String parentClassName = ((SpanElement) parent.cast()).getClassName();
            if (parentClassName.indexOf(controller.getDecorateClassName()) != -1) {
                className = parentClassName + " "
                        + AnnotationConstant.DECORATE_CLASS_NAME
                        + annotation.getId();
            }
        }
        spanElement.setClassName(className);
        insertBefore(parent, currentNode.getNextSibling(), spanElement);
        return spanElement;
    }

    protected void decorateNode() {
        if (!(currentNode.getNodeType() == Node.TEXT_NODE)) {
            return;
        }
        Text text = (Text) currentNode;
        String data = text.getData();
        decorateText(data);
        currentNode.getParentNode().removeChild(currentNode);
    }

    protected SpanElement getSpanElement(Document document) {
        SpanElement spanElement = document.createSpanElement();
        DOM.sinkEvents((Element) spanElement.cast(), Event.ONMOUSEOVER
                | Event.ONMOUSEOUT);
        DOM.setEventListener((Element) spanElement.cast(),
                AnnotationPopupEventListener.getAnnotationPopupEventListener(
                        annotation, controller));
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
        return endOffset <= 0;
    }

}
