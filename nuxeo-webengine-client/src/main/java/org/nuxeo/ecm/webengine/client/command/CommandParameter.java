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

package org.nuxeo.ecm.webengine.client.command;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandParameter {
    protected String key;
    protected String value;
    protected CommandToken token;

    public CommandParameter(String key, CommandToken token) {
        this.key = key;
        this.token = token;
    }
    public String getKey() {
        return key;
    }

    public CommandToken getToken() {
        return token;
    }

    public String getValue() {
        return value;
    }

    public String getValueOrDefault() {
        return value == null ? token.defaultValue : value;
    }

    @Override
    public String toString() {
        return key+" = "+value;
    }
}
