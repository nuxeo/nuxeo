/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import java.net.URI;
import java.util.List;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;

/**
 * @author Alexandre Russel
 */
public interface Annotation {

    Resource getSubject();

    String getId();

    URI getAnnotates();

    String getContext();

    URI getBody();

    String getBodyAsText();

    void setBody(Statement body);

    void setSubject(Resource resource);

    List<Statement> getStatements();

    void setStatements(List<Statement> annotationStatements);

    void setAnnotates(Statement statement);

    void setContext(Statement statement);

    String getCreator();

    void addMetadata(String predicate, String value);

    void setBodyText(String string);

}
