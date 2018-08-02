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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory;

import java.util.List;

/**
 * Base interface for references between directory fields.
 * <p>
 * References are used to leverage SQL joins or attributes that store a list of distinguished names in LDAP servers
 * (e.g. uniqueMember).
 * <p>
 * In nuxeo directories, references are special entry fields that are string list of entry ids of a target directory.
 *
 * @author ogrisel
 */
public interface Reference {

    String getFieldName();

    Directory getSourceDirectory();

    void setSourceDirectoryName(String sourceDirectoryName);

    Directory getTargetDirectory();

    /**
     * @deprecated since 9.2 (unused)
     */
    @Deprecated
    void setTargetDirectoryName(String targetDirectoryName);

    void addLinks(String sourceId, List<String> targetIds);

    void addLinks(List<String> sourceIds, String targetId);

    void removeLinksForSource(String sourceId);

    void removeLinksForTarget(String targetId);

    List<String> getTargetIdsForSource(String sourceId);

    List<String> getSourceIdsForTarget(String targetId);

    void setTargetIdsForSource(String sourceId, List<String> targetIds);

    void setSourceIdsForTarget(String targetId, List<String> sourceIds);

    /**
     * Adds the links between the source id and the target ids
     *
     * @param sourceId the source id
     * @param targetIds the target ids
     * @param session the session
     * @since 9.2
     */
    void addLinks(String sourceId, List<String> targetIds, Session session);

    /**
     * Adds the links between the source ids and the target id
     *
     * @param sourceIds the source ids
     * @param targetId the target id
     * @param session the session
     * @since 9.2
     */
    void addLinks(List<String> sourceIds, String targetId, Session session);

    /**
     * Sets all source ids to be associated to the given target id
     *
     * @param targetId the target id
     * @param sourceIds the source ids
     * @param session the session
     * @since 9.2
     */
    void setSourceIdsForTarget(String targetId, List<String> sourceIds, Session session);

    /**
     * Sets all target ids to be associated to the given source id
     *
     * @param sourceId the source id
     * @param targetIds the target ids
     * @param session the session
     * @since 9.2
     */
    void setTargetIdsForSource(String sourceId, List<String> targetIds, Session session);

    /**
     * Removes all the links for a given target id
     *
     * @param targetId the target id
     * @param session the session
     * @since 9.2
     */
    void removeLinksForTarget(String targetId, Session session);

    /**
     * Removes all the links for a given source id
     *
     * @param sourceId the source id
     * @param session the session
     * @since 9.2
     */
    void removeLinksForSource(String sourceId, Session session);

}
