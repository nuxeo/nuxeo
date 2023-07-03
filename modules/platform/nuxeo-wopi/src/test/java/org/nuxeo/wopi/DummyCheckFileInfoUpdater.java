/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.wopi;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2021.40
 */
public class DummyCheckFileInfoUpdater implements CheckFileInfoUpdater {

    @Override
    public Map<String, Serializable> update(Map<String, Serializable> checkFileInfoProperties) {
        return checkFileInfoProperties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> "foobar"));
    }
}
