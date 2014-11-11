/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
