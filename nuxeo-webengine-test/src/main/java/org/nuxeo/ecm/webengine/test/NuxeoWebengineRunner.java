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
package org.nuxeo.ecm.webengine.test;

import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.core.test.guice.CoreModule;
import org.nuxeo.ecm.platform.test.NuxeoPlatformRunner;
import org.nuxeo.ecm.platform.test.PlatformModule;
import org.nuxeo.runtime.test.runner.RuntimeModule;

import com.google.inject.Module;

public class NuxeoWebengineRunner extends NuxeoPlatformRunner {

    public NuxeoWebengineRunner(Class<?> classToRun) throws InitializationError {
        // FIXME: There's surely a better way to inherit from parent modules...
        this(classToRun, new RuntimeModule(), new CoreModule(),
                new PlatformModule(), new WebengineModule());
    }

    public NuxeoWebengineRunner(Class<?> classToRun, Module... modules)
            throws InitializationError {
        super(classToRun, modules);
    }

}
