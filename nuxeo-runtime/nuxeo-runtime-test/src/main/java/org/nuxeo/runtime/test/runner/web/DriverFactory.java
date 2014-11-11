/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime.test.runner.web;

import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DriverFactory {

    /**
     * Get the family of the driver this factory can create
     * @return
     */
    public BrowserFamily getBrowserFamily();
    
    /**
     * Create the driver
     * @return
     */
    public WebDriver createDriver();
    
    /**
     * Dispose any needed resources after the driver was closed
     * @param driver
     */
    public void disposeDriver(WebDriver driver);
    
}
