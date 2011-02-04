/*
 * (C) Copyright 2007-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        List<String> list = new ArrayList<String>();
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
        for (String element:(String[])getValue()) {
            text += sep + element;
            sep = ", ";
        }
        return text;
    }
}
