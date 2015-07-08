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
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.negotiation;

import java.util.Map;

/**
 * Interface for negotiator classes.
 *
 * @since 7.4
 */
public interface Negotiator {

    /**
     * Should return null if next negotiator should apply.
     */
    String getResult(String target, Object context);

    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    String getProperty(String name);

    String getProperty(String name, String defaultValue);

}
