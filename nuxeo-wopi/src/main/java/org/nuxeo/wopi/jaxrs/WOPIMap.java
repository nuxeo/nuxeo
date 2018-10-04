/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi.jaxrs;

import java.io.Serializable;
import java.util.Map;

/**
 * Custom object returned by WOPI endpoints wrapping a Map object to make it writable.
 *
 * @since 10.3
 */
public class WOPIMap {

    protected final Map<String, Serializable> map;

    protected WOPIMap(Map<String, Serializable> map) {
        this.map = map;
    }

    public static WOPIMap of(Map<String, Serializable> map) {
        return new WOPIMap(map);
    }

}
