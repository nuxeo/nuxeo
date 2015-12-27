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
 */
public class NuxeoSelectedTextDecoratorVisitor extends NuxeoDecoratorVisitor {

    public NuxeoSelectedTextDecoratorVisitor(Annotation annotation, AnnotationController controller) {
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
