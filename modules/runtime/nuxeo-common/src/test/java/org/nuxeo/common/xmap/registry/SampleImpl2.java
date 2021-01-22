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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

/**
 * Sample implementation without a no-argument constructor that can be handled by xmap.
 *
 * @since 11.5
 */
public class SampleImpl2 implements SampleInterface {

    protected String init;

    public SampleImpl2(String init) {
        this.init = init;
    }

    @Override
    public boolean doSomething() {
        throw new UnsupportedOperationException();
    }

}
