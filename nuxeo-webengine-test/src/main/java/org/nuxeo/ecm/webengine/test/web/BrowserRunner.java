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
package org.nuxeo.ecm.webengine.test.web;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import org.nuxeo.ecm.webengine.test.NuxeoWebengineRunner;

import com.google.inject.Binder;
import com.google.inject.Scopes;

public class BrowserRunner extends NuxeoWebengineRunner {

    private static final Log log = LogFactory.getLog(BrowserRunner.class);

    public BrowserRunner(Class<?> classToRun) throws InitializationError {
        super(classToRun);
        try {
            BrowserConfig config = getInjector().getInstance(
                    BrowserConfig.class);
            final String browserType = config.getBrowser();
            filter(new Filter() {

                @Override
                public boolean shouldRun(Description description) {
                    SkipBrowser skip = description.getAnnotation(SkipBrowser.class);
                    if (skip != null
                            && Arrays.asList(skip.value()).contains(browserType)) {

                        return false;
                    }
                    return true;
                }

                @Override
                public String describe() {
                    return "Filtering tests according to current browser settings";
                }
            });
        } catch (ClassCastException e) {
            // OK - just skip
        } catch (NoTestsRemainException e) {
            log.error(e.toString(), e);
        }
    }

    @Override
    protected void configure(Binder binder) {
        super.configure(binder);
        binder.bind(BrowserConfig.class).to(StandardBrowserConfig.class).in(
                Scopes.SINGLETON);
    }
}
