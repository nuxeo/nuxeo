/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

/**
 * @since 5.8
 */
public class RemoteThrowable extends Throwable {

    private static final long serialVersionUID = 1L;

    public RemoteThrowable(String message) {
        super(message);
    }

    protected final HashMap<String, JsonNode> otherNodes = new HashMap<String, JsonNode>();

    public Map<String, JsonNode> getOtherNodes() {
        return otherNodes;
    }
}
