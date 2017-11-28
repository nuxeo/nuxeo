/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.uidgen;

/**
 * @since 7.4
 */
public abstract class AbstractUIDSequencer implements UIDSequencer {

    protected String name;

    @Override
    public int getNext(String key) {
        return (int) getNextLong(key);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void initSequence(String key, long id) {
        while (getNextLong(key) < id) {
            continue;
        }
    }

    @Override
    public void initSequence(String key, int id) {
        initSequence(key, (long) id);
    }

}
