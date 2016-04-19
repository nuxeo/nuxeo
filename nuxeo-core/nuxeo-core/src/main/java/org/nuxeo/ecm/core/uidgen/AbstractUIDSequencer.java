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
    public abstract void init();

    @Override
    public abstract int getNext(String key);

    @Override
    public long getNextLong(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public abstract void dispose();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void initSequence(String key, int id) {
        while ((getNext(key)) < id) {
            continue;
        }
    }

}
