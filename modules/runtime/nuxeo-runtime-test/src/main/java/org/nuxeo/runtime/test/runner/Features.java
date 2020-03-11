/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A feature is Nuxeo Runner extension that is able to configure the runner from additional annotations and using a
 * specific logic.
 * <p>
 * Let say you want a test that launches a Nuxeo Core with webengine and webdriver enabled. You can activate these
 * features using the Feature annotation like this:
 *
 * <pre>
 * &#064;RunWith(NuxeoRunner.class)
 * &#064;Features({ CoreFeature.class, WebDriverFeature.class, WebEngineFeature.class })
 * public class MyTest {
 *
 * }
 * </pre>
 *
 * or use the <code>@Features</code> annotation on an interface or subclass of your test class. All the features
 * presents on the class hierarchy will be collected and used.
 * <p>
 * Features must implement RunnerFeature class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Features {

    Class<? extends RunnerFeature>[] value();

}
