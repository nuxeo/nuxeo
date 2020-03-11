/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository;

import java.util.concurrent.Callable;

/**
 * Low-level Repository factory.
 * <p>
 * The repository factory to use is usually specified in the MBean configuration file.
 */
public interface RepositoryFactory extends Callable<Object> {

    /**
     * Constructs the low-level repository with the name previously passed to {@code init}.
     *
     * @return a low-level Repository
     */
    @Override
    Object call();

}
