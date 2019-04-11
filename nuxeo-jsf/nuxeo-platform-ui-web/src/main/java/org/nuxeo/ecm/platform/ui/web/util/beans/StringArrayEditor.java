/*
 * (C) Copyright 2007-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 *
 */

package org.nuxeo.ecm.platform.ui.web.util.beans;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringArrayEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(text, ",");
        List<String> list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        String[] value = new String[list.size()];
        setValue(list.toArray(value));
    }

    @Override
    public String getAsText() {
        String sep = "";
        String text = "";
        for (String element : (String[]) getValue()) {
            text += sep + element;
            sep = ", ";
        }
        return text;
    }
}
