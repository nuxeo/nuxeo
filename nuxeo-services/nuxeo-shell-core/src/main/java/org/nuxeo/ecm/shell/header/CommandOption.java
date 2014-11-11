/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.header;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandOption {

    public String[] names;
    public String type;
    public boolean isRequired;
    public String defaultValue;

    @Override
    public String toString() {
        String str = StringUtils.join(names, '|');
        if (type != null) {
            str += ":" + type;
        }
        if (defaultValue != null) {
            str += "?" + defaultValue;
        }
        return !isRequired ? "[" +str+"]" : str;
    }

}
