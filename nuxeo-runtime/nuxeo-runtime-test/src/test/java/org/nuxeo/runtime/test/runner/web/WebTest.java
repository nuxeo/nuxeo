/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner.web;

import static org.junit.Assert.assertNotNull;

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

    @Test
    public void testSearch() {
        SearchResultPage result = home.search("test");
        assertNotNull(result.getFirstResult());
//        System.out.println(result.getFirstResult());
    }

}
