/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Provider;
/**
 * Should be removed
 * @deprecated replaced by direct instance binding see {@link RuntimeModule} 
 */
@Deprecated
public class RuntimeHarnessProvider implements Provider<RuntimeHarness> {

    private static final Log log = LogFactory.getLog(RuntimeHarnessProvider.class);

    public RuntimeHarness get() {
        try {
            return NuxeoRunner.getRuntimeHarness();
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

}
