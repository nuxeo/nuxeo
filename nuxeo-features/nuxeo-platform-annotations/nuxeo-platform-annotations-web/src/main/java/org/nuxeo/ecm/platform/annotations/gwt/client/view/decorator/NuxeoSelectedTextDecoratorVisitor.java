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
package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class NuxeoSelectedTextDecoratorVisitor extends NuxeoDecoratorVisitor {

    public NuxeoSelectedTextDecoratorVisitor(Annotation annotation,
            AnnotationController controller) {
        super(annotation, controller);
    }

    @Override
    protected Element decorateTextWithSpan(String data) {
        if (data.trim().length() == 0) {
            // don't add span to empty text
            return null;
        }

        Document document = currentNode.getOwnerDocument();
        SpanElement spanElement = getSpanElement(document);
        spanElement.setInnerText(data);
        Node parent = currentNode.getParentNode();
        String className = AnnotationConstant.SELECTED_TEXT_CLASS_NAME;
        if (parent.getNodeName().equalsIgnoreCase("span")) {
            String parentClassName = ((SpanElement) parent.cast()).getClassName();
            if (parentClassName.indexOf(AnnotationConstant.SELECTED_TEXT_CLASS_NAME) != -1) {
                className = parentClassName + AnnotationConstant.SELECTED_TEXT_CLASS_NAME;
            }
        }
        spanElement.setClassName(className);
        insertBefore(parent, currentNode.getNextSibling(), spanElement);
        return spanElement;
    }

    @Override
    protected SpanElement getSpanElement(Document document) {
        SpanElement spanElement = document.createSpanElement();
        return spanElement;
    }

}
