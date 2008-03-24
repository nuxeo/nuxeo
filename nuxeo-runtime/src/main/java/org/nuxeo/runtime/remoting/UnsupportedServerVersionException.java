/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.remoting;

import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.config.ConfigurationException;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class UnsupportedServerVersionException extends ConfigurationException {

    private static final long serialVersionUID = 7632769314648547250L;

    private final Version version;

    public UnsupportedServerVersionException(Version version) {
        super("Unsupported server configuration version: " + version);
        this.version = version;
    }

    public UnsupportedServerVersionException(Version version, String message) {
        super("Unsupported server configuration version: " + version + ". " + message);
        this.version = version;
    }

}
