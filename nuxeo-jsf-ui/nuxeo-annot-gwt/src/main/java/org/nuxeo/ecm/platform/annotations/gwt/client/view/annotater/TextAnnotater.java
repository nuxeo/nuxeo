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

package org.nuxeo.ecm.platform.annotations.gwt.client.view.annotater;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Container;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Range;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPathUtil;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.NewAnnotationPopup;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Event;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class TextAnnotater extends AbstractAnnotater {

    private final XPathUtil xpathUtil = new XPathUtil();

    public TextAnnotater(AnnotationController controller) {
        super(controller, false);
    }

    @Override
    public void onMouseUp(Event event) {
        Log.debug("TextAnnotater#onMouseUp; eventId= " + event.getType() + "; source: " + event.getCurrentTarget());
        Range currentRange = Utils.getCurrentRange(Document.get());
        if (currentRange != null && currentRange.getSelectedText().length() != 0) {
            Element startElement = Element.as(currentRange.getStartContainer());
            String pointer = xpathUtil.getSelectionXPointer(currentRange);
            controller.createNewAnnotation(pointer);

            Container startContainer = getStartContainer(currentRange);
            Container endContainer = getEndContainer(currentRange);

            Annotation annotation = controller.getNewAnnotation();
            annotation.setStartContainer(startContainer);
            annotation.setEndContainer(endContainer);

            NewAnnotationPopup popup = new NewAnnotationPopup(startElement, controller, false, "local");
            controller.setNewAnnotationPopup(popup);
            addAnnotationPopup();
        } else {
            controller.setNewAnnotationPopup(null);
        }

        super.onMouseUp(event);
    }

    private Container getStartContainer(Range range) {
        Node startNode = range.getStartContainer();
        int startOffset = range.getStartOffset();
        // Window.alert("startOffset: " + startOffset);
        startOffset = computeNewOffset(startNode, startOffset);
        // Window.alert("startOffset after compute: " + startOffset);

        if (startNode.getNodeType() == Node.TEXT_NODE) {
            return getCustomContainer(startNode, startOffset);
        }
        return new Container(xpathUtil.getXPath(startNode), startOffset);
    }

    private Container getCustomContainer(Node node, int currentOffset) {
        int offset = 0;
        Node n = node.getPreviousSibling();
        while (n != null) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                Text text = (Text) n;
                offset += text.getLength();
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element ele = (Element) n;
                offset += ele.getInnerText().length();
            }
            n = n.getPreviousSibling();
        }
        node = node.getParentNode();
        currentOffset += offset;
        return new Container(xpathUtil.getXPath(node), currentOffset);
    }

    private static int computeNewOffset(Node node, int currentOffset) {
        if (currentOffset <= 0) {
            return currentOffset;
        }
        int difference = 0;
        String text = node.getNodeValue();
        if (text != null) {
            text = text.substring(0, currentOffset);
            String processedText = Utils.removeWhitespaces(text, node, true);
            difference = text.length() - processedText.length();
        }
        return currentOffset - difference;
    }

    private Container getEndContainer(Range range) {
        Node endNode = range.getEndContainer();
        int endOffset = range.getEndOffset();
        endOffset = computeNewOffset(endNode, endOffset);

        if (endNode.getNodeType() == Node.TEXT_NODE) {
            return getCustomContainer(endNode, endOffset);
        }

        return new Container(xpathUtil.getXPath(endNode), endOffset);
    }

}
