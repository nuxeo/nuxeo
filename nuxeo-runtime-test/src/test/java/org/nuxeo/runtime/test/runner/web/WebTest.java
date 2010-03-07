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

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Ignore("this is making remote connections - it serves only for demonstrating webdriverfeature")
@RunWith(FeaturesRunner.class)
@Features(WebDriverFeature.class)
@HomePage(type=MyHomePage.class, url="http://www.google.com")
@Browser(type=BrowserFamily.HTML_UNIT)
public class WebTest {

    @Inject protected MyHomePage home;

    @Test public void testSearch() throws Exception {
        SearchResultPage result = home.search("test");
        Assert.assertNotNull(result.getFirstResult());
//        System.out.println(result.getFirstResult());
    }

}
