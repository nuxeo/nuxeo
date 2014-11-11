/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.impl;

import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.ValueAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DefaultValueAdapter implements ValueAdapter {

    @SuppressWarnings("unchecked")
    public <T> T getValue(Shell shell, Class<T> type, String value) {
        if (type == CharSequence.class || type == String.class) {
            return (T) value;
        }
        if (type.isPrimitive()) {
            if (type == Boolean.TYPE) {
                return (T) Boolean.valueOf(value);
            } else if (type == Integer.TYPE) {
                return (T) Integer.valueOf(value);
            } else if (type == Float.TYPE) {
                return (T) Float.valueOf(value);
            } else if (type == Long.TYPE) {
                return (T) Long.valueOf(value);
            } else if (type == Double.TYPE) {
                return (T) Double.valueOf(value);
            } else if (type == Character.TYPE) {
                return (T) (Character.valueOf(value == null
                        || value.length() == 0 ? '\0' : value.charAt(0)));
            }
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (Number.class.isAssignableFrom(type)) {
            if (type == Integer.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Float.class) {
                return (T) Float.valueOf(value);
            } else if (type == Long.class) {
                return (T) Long.valueOf(value);
            } else if (type == Double.class) {
                return (T) Double.valueOf(value);
            }
        } else if (type == Character.class) {
            return (T) (Character.valueOf(value == null || value.length() == 0 ? '\0'
                    : value.charAt(0)));
        } else if (CommandType.class.isAssignableFrom(type)) {
            CommandType cmd = shell.getActiveRegistry().getCommandType(value);
            if (cmd == null) {
                throw new ShellException("Unknown command: " + value);
            }
            return (T) cmd;
        }
        return null;

    }

}
