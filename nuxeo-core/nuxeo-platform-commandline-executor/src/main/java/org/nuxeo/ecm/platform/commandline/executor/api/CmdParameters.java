/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Vincent Dutat
 *     Vladimir Pasquier
 *     Julien Carsique
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps command parameters (String or File path, or a list of values that will be expanded as separate parameters).
 */
public class CmdParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, ParameterValue> params = new HashMap<>();

    /**
     * It is recommended to use the CmdParameters instance returned by
     * {@link CommandLineExecutorService#getDefaultCmdParameters()} which is initialized with a few common parameters.
     *
     * @see CommandLineExecutorService#getDefaultCmdParameters()
     */
    public CmdParameters() {
    }

    public void addNamedParameter(String name, String value) {
        params.put(name, new ParameterValue(value));
    }

    public void addNamedParameter(String name, File file) {
        addNamedParameter(name, file.getAbsolutePath());
    }

    /**
     * @since 7.10
     */
    public void addNamedParameter(String name, List<String> values) {
        params.put(name, new ParameterValue(values));
    }

    /**
     * @since 7.10
     */
    public String getParameter(String name) {
        ParameterValue param = params.get(name);
        return param == null ? null : param.getValue();
    }

    /**
     * @since 7.1
     */
    public Map<String, ParameterValue> getParameters() {
        return params;
    }

    /**
     * A parameter value holding either a single String or a list of Strings.
     *
     * @since 7.10
     */
    public static class ParameterValue {

        private final String value;

        private final List<String> values;

        public ParameterValue(String value) {
            this.value = value;
            values = null;
        }

        /**
         * @since 7.10
         */
        public ParameterValue(List<String> values) {
            this.values = values;
            value = null;
        }

        public String getValue() {
            return value;
        }

        public List<String> getValues() {
            return values;
        }

        /**
         * Checks whether this is multi-valued.
         *
         * @since 7.10
         */
        public boolean isMulti() {
            return values != null;
        }
    }

}
