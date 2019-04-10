/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.processors;

public class HtmlBodyExtractor {

    protected final static String BODY_DELIMITER = "</{0,1}[bB][oO][dD][yY][^>]*>";

    public static String extractHtmlBody(String htmlContent) {

        if (htmlContent != null) {
            String[] parts = htmlContent.split(BODY_DELIMITER);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return htmlContent;
    }

}
