/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.model;

/**
 * Some default application start orders.
 *
 * @since 9.3
 */
public class ComponentStartOrders {

    // @since 2021.14
    public static final int MONGODB = 40;

    // @since 2021.14
    public static final int ELASTIC = 50;

    public static final int REPOSITORY = 100;

    public static final int DEFAULT = 1000;

}
