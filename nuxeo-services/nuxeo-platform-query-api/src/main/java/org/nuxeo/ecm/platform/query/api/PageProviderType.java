/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer<ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.query.api;

/**
 * Available types for a {@link PageProvider}.
 * 
 * @since 2021.8
 */
public enum PageProviderType {

    DEFAULT("default"), ELASTIC("elastic");

    private String name;

    private PageProviderType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
