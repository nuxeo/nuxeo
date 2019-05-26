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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationUtils {
    private AnnotationUtils() {
        // Helper class
    }

    public static String escapeHtml(String maybeHtml) {
        final Element div = DOM.createDiv();
        DOM.setInnerText(div, maybeHtml);
        String escapedHtml = DOM.getInnerHTML(div).replaceAll("<BR>", "\n")
        // IE keep '&nbsp;' which will raise an error in the RDF backend
        .replaceAll("&nbsp;", " ");
        return escapedHtml;
    }

    public static String replaceCarriageReturns(String text) {
        return text.replaceAll("\n", "<br/>");
    }

}
