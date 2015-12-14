/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps command parameters (String or File). Quoted by default. Use {@link #addNamedParameter(String, String, boolean)}
 * to avoid quotes.
 *
 * @author tiry
 * @author Vincent Dutat
 */
public class CmdParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Map<String, String> params;

    private final HashMap<String, CmdParameter> cmdParameters;

    public CmdParameters() {
        params = new HashMap<>();
        cmdParameters = new HashMap<>();
    }

    public void addNamedParameter(String name, String value) {
        params.put(name, value);
        // Quote by default
        CmdParameter cmdParameter = new CmdParameter(value, true);
        cmdParameters.put(name, cmdParameter);
    }

    public void addNamedParameter(String name, File file) {
        addNamedParameter(name, file.getAbsolutePath());
    }

    public Map<String, String> getParameters() {
        return params;
    }

    /**
     * @since 7.1
     */
    public void addNamedParameter(String name, String value, boolean quote) {
        params.put(name, value);
        CmdParameter cmdParameter = new CmdParameter(value, quote);
        cmdParameters.put(name, cmdParameter);
    }

    /**
     * @since 7.1
     */
    public void addNamedParameter(String name, File file, boolean quote) {
        addNamedParameter(name, file.getAbsolutePath(), quote);
    }

    /**
     * @since 7.1
     */
    public HashMap<String, CmdParameter> getCmdParameters() {
        return cmdParameters;
    }

    /**
     * @since 7.1
     */
    public class CmdParameter {

        private final String value;

        private final boolean quote;

        public CmdParameter(String value, boolean quote) {
            this.value = value;
            this.quote = quote;
        }

        public String getValue() {
            return value;
        }

        public boolean isQuote() {
            return quote;
        }
    }
}
