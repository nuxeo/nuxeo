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
package org.nuxeo.ecm.platform.test.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.nuxeo.runtime.test.runner.NuxeoRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.openqa.selenium.WebDriver;

import com.google.inject.Binder;
import com.google.inject.Provider;

public class WebDriverFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(WebDriverFeature.class);

    protected BrowserConfig config; 
    
    public void initialize(NuxeoRunner runner, Class<?> testClass)
            throws Exception {
        Browser browser = testClass.getAnnotation(Browser.class);
        if (browser == null) {
            config = new StandardBrowserConfig();
        } else {
            config = StandardBrowserConfig.fromAnnotation(browser); 
        }        
        try {
            runner.filter(new Filter() {

                @Override
                public boolean shouldRun(Description description) {
                    SkipBrowser skip = description.getAnnotation(SkipBrowser.class);
                    if (skip == null) {
                        return true;
                    }
                    for (BrowserFamily family : skip.value()) {
                        if (config.getFamily().equals(family)) {
                            return false;
                        }
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
    public void configure(NuxeoRunner runner, Binder binder) {
        binder.bind(BrowserConfig.class).toInstance(config);
        binder.bind(WebDriver.class).toProvider(new Provider<WebDriver>() {
            public WebDriver get() {
                return config.getDriver();
            }
        });
    }
}
