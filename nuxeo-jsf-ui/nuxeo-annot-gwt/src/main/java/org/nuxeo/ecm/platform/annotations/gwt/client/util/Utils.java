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

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

/**
 * @author Alexandre Russel
 */
public class Utils {

    public static native Document setDocument(Document document) /*-{
                                                                 $temp = $doc;
                                                                 $doc = document;
                                                                 return $temp
                                                                 }-*/;

    public static int[] getAbsoluteTopLeft(Element element, Document document) {
        int[] result = new int[2];
        Document doc = Utils.setDocument(document);
        result[0] = element.getAbsoluteTop();
        result[1] = element.getAbsoluteLeft();
        Utils.setDocument(doc);
        return result;
    }

    public static native String getBaseHref() /*-{
                                              return top['baseHref'];
                                              }-*/;

    public static native Range getCurrentRange(Document document) /*-{
                                                                  if( document &&
                                                                  document.defaultView &&
                                                                  document.defaultView.getSelection() &&
                                                                  document.defaultView.getSelection().getRangeAt(0)) {
                                                                  // W3C Range
                                                                  var userSelection = document.defaultView.getSelection().getRangeAt(0);
                                                                  var range = @org.nuxeo.ecm.platform.annotations.gwt.client.util.Range::new(Ljava/lang/String;Lcom/google/gwt/dom/client/Node;ILcom/google/gwt/dom/client/Node;I)(userSelection.toString(), userSelection.startContainer, userSelection.startOffset, userSelection.endContainer, userSelection.endOffset);
                                                                  return range;
                                                                  } else if(document.selection) {
                                                                  // IE TextRange
                                                                  var ieSelection = document.selection.createRange();
                                                                  var ieRange = new $wnd.InternetExplorerRange(ieSelection);
                                                                  ieRange._init();
                                                                  var range = @org.nuxeo.ecm.platform.annotations.gwt.client.util.Range::new(Ljava/lang/String;Lcom/google/gwt/dom/client/Node;ILcom/google/gwt/dom/client/Node;I)(ieSelection.text, ieRange.startContainer, ieRange.startOffset, ieRange.endContainer, ieRange.endOffset);
                                                                  return range;
                                                                  }
                                                                  return null;
                                                                  }-*/;

    public static String removeWhitespaces(String text, Node node) {
        return removeWhitespaces(text, node, false);
    }

    public static String removeWhitespaces(String text, Node node, boolean forceIfOnlyWhitespaces) {
        if (text == null) {
            return "";
        }
        if (!forceIfOnlyWhitespaces) {
            if (text.matches("^\\s+$")) {
                return text;
            }
        }
        // Window.alert("Before removeWS: " + text);
        Element prevSibling = (Element) node.getPreviousSibling();

        String processedText = text;
        if (prevSibling == null
                || !(new CSSClassManager(prevSibling).isClassPresent(AnnotationConstant.IGNORED_ELEMENT))) {
            processedText = processedText.replaceAll("^\\s+", "");
        }
        // Window.alert("in progress removeWS: " + processedText);
        processedText = processedText.replaceAll("\\s+", " ");
        // Window.alert("after removeWS: " + processedText);
        return processedText;
    }
}
