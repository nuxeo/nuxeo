/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 * $Id$
 */
package org.nuxeo.runtime.test.runner;

import org.junit.runner.Description;

public class Settings {

    private final Description description;

    public Settings(Description description) {
        this.description = description;
    }


    public String[] getBundles() {
        Bundles annotation = description.getAnnotation(Bundles.class);
        if(annotation != null) {
            return annotation.value();
        } else {
            return new String[0];
        }
    }
}
