/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: GraphDescription.java 19245 2007-05-23 18:10:54Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.util.Map;

/**
 * Interface for a graph description.
 * <p>
 * A graph description gives all the information needed for the graph instantiation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface GraphDescription {

    String getName();

    String getGraphType();

    Map<String, String> getOptions();

    Map<String, String> getNamespaces();

}
