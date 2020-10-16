/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.management.stopwatchs;

import java.io.Serializable;
import java.util.Map;

/**
 * @author matic
 * @deprecated since 11.4: use dropwizard metrics timer instead
 */
@Deprecated(since = "11.4")
public interface StopwatchMXBean extends org.javasimon.jmx.StopwatchMXBean {

    String sampleAsString();

    Map<String, Serializable> sampleAsMap();

}
