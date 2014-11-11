/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandLineOptions {

    private final LinkedHashMap<Object, String> args = new LinkedHashMap<Object, String>();


    public CommandLineOptions(String[] args) {
        String op = null;
        int i = 0;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                op = arg.substring(1);
                this.args.put(op, "");
            } else if (op != null) {
                this.args.put(op, arg);
                op = null;
            } else {
                this.args.put(i++, arg);
                op = null;
            }
        }
    }

    public String getArg(int i) {
        return args.get('#' + String.valueOf(i));
    }

    public String getOption(String option) {
        return args.get(option);
    }

    public boolean hasOption(String option) {
        return args.containsKey(option);
    }

    public LinkedHashMap<Object, String> getOptions() {
        return args;
    }

    public Set<Map.Entry<Object,String>> entrySet() {
        return args.entrySet();
    }

}
