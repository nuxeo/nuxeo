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
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.nuxeo.osgi.OSGiAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandLineOptions {

    private final LinkedHashMap<String, String> args = new LinkedHashMap<String, String>();

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
                this.args.put("#".concat(Integer.toString(i++)), arg);
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

    protected static class OptionKey {

        protected final String fromKey;

        protected final String toKey;

        protected OptionKey(String fromKey, String toKey) {
            this.fromKey = fromKey;
            this.toKey = toKey;
        }

        protected boolean map(String key, String value, Properties props) {
            if (!fromKey.equals(key)) {
                return false;
            }
            props.put(toKey, value);
            return true;
        }
    }

    protected final OptionKey[] keys = new OptionKey[] {
            new OptionKey("host", OSGiAdapter.HOST_NAME),
            new OptionKey("version", OSGiAdapter.HOST_VERSION),
            new OptionKey("home", OSGiAdapter.HOME_DIR),
            new OptionKey("log", OSGiAdapter.LOG_DIR),
            new OptionKey("data", OSGiAdapter.DATA_DIR),
            new OptionKey("tmp", OSGiAdapter.TMP_DIR),
            new OptionKey("web", OSGiAdapter.WEB_DIR),
            new OptionKey("config", OSGiAdapter.CONFIG_DIR),
            new OptionKey("libs", OSGiAdapter.LIBS),
            new OptionKey("bundles", OSGiAdapter.BUNDLES),
            new OptionKey("devmode", OSGiAdapter.DEVMODE),
            new OptionKey("preprocessing", OSGiAdapter.PREPROCESSING),
            new OptionKey("scanForNestedJars", OSGiAdapter.SCAN_FOR_NESTED_JARS),
            new OptionKey("flushCache", OSGiAdapter.FLUSH_CACHE),
            new OptionKey("args", OSGiAdapter.ARGS)
    };

    protected void mapOption(String option, String value, Properties props) {
        for (OptionKey key:keys) {
            if (key.map(option,value,props)) {
                return;
            }
        }
        if (!option.startsWith("#")) {
            option = "?".concat(option);
        }
        props.put(option,value);
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        for (Entry<String,String> entry:args.entrySet()) {
            mapOption(entry.getKey(), entry.getValue(), properties);
        }
        return properties;
    }
}
