/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CommandLineOptions {

    private final LinkedHashMap<Object, String> args = new LinkedHashMap<>();

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

    public Set<Map.Entry<Object, String>> entrySet() {
        return args.entrySet();
    }

}
