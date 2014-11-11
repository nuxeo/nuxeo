/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.common.utils.i18n;

import java.io.Serializable;

public class Labeler implements Serializable {

    private static final long serialVersionUID = -4139432411098427880L;

    protected final String prefix;

    public Labeler(String prefix) {
        this.prefix = prefix;
    }

    protected static String unCapitalize(String s) {
        char c = Character.toLowerCase(s.charAt(0));
        return c + s.substring(1);
    }

    public String makeLabel(String itemId) {
        return prefix + '.' + unCapitalize(itemId);
    }

}
