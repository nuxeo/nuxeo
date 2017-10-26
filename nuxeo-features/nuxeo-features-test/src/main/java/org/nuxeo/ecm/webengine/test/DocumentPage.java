/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.test;

import org.nuxeo.runtime.test.runner.web.Attachment;
import org.nuxeo.runtime.test.runner.web.WebPage;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 9.3
 */
@Deprecated
public class DocumentPage extends WebPage {

    public String getTitle() {
        return findElement(By.id("tab_content")).findElement(By.tagName("h2")).getText().trim();
    }

    public Attachment download(String name) {
        findElement(By.id("tab_content")).findElement(By.linkText(name)).click();
        return getAttachment();
    }
}
