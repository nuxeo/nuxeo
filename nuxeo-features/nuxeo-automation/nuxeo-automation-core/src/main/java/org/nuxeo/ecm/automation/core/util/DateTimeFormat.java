/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */
package org.nuxeo.ecm.automation.core.util;

/**
 * Enum describing the format to use when marshaling a date into JSON.
 *
 * @since 7.1
 */
public enum DateTimeFormat {

    /**
     * Marshals the date as a W3C date (ISO 8601).
     * <p>
     * Example: {@code 2011-10-23T12:00:00.00Z}.
     */
    W3C,

    /**
     * Marshals the date as a number of milliseconds since epoch.
     */
    TIME_IN_MILLIS;

}
