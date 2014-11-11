/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

import java.io.File;
import java.io.FilenameFilter;

public class CustomThemeNameFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return name.matches("^theme\\-([a-z]|[a-z][a-z0-9_\\-]*?[a-z0-9])\\.xml$");
    }

}
