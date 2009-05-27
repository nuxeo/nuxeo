/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.nuxeo.ecm.platform.tag.entity;

/**
 * Utility class used for managing constants used in tagging.
 * 
 * @author rux
 */
public class TaggingConstants {

    /**
     * Utility constants that describe the NXP_TAGGING table.
     */

    public static final String TAGGING_TABLE_NAME = "NXP_TAGGING";

    public static final String TAGGING_TABLE_COLUMN_ID = "ID";

    public static final String TAGGING_TABLE_COLUMN_TAG_ID = "TAG_ID";

    public static final String TAGGING_TABLE_COLUMN_AUTHOR = "AUTHOR";

    public static final String TAGGING_TABLE_COLUMN_DOCUMENT_ID = "DOCUMENT_ID";

    public static final String TAGGING_TABLE_COLUMN_CREATION_DATE = "CREATION_DATE";

    public static final String TAGGING_TABLE_COLUMN_IS_PRIVATE = "IS_PRIVATE";

    /**
     * Utility constants used to manage the defined named queries.
     */
    public static final String LIST_TAGS_FOR_DOCUMENT = "listTagsForDocument";

    public static final String LIST_TAGS_FOR_DOCUMENT_QUERY = "SELECT DISTINCT tag.id, tag.label FROM Tagging tagging "
            + "JOIN tagging.tag tag JOIN tag.hierarchy h JOIN h.dublincore dc "
            + "WHERE (tagging.isPrivate=false OR tagging.author=:userName) AND "
            + "(tag.private1 = false OR dc.creator = :userName) AND "
            + "tagging.targetDocument.id = :targetId";

    public static final String LIST_DOCUMENTS_FOR_TAG = "listDocumentsForTag";

    public static final String LIST_DOCUMENTS_FOR_TAG_QUERY = "SELECT DISTINCT tagging.targetDocument.id FROM Tagging tagging "
            + "WHERE (tagging.isPrivate=false OR tagging.author=:userName) AND "
            + "tagging.tag.id = :tagId";

    public static final String LIST_TAGS_FOR_DOCUMENT_AND_USER = "listTagsForDocumentAndUser";

    public static final String LIST_TAGS_FOR_DOCUMENT_AND_USER_QUERY = "SELECT DISTINCT tagging.id, tag.label FROM Tagging tagging "
            + "JOIN tagging.tag tag JOIN tag.hierarchy h JOIN h.dublincore dc "
            + "WHERE tagging.author=:userName AND "
            + "(tag.private1 = false OR dc.creator = :userName) AND "
            + "tagging.targetDocument.id = :targetId";

    public static final String GET_TAGGING = "getTagging";

    public static final String GET_TAGGING_QUERY = "SELECT COUNT(tagging.id) FROM Tagging tagging "
            + "WHERE tagging.tag.id = :tagId AND tagging.targetDocument.id = :docId AND "
            + "tagging.author = :userName";

    public static final String GET_VOTE_TAG = "getVoteTag";

    public static final String GET_VOTE_TAG_QUERY = "SELECT COUNT(tagging) FROM Tagging tagging "
            + "WHERE tagging.targetDocument.id=:targetId AND tagging.tag.id=:tagId AND "
            + "(tagging.isPrivate=false OR tagging.author=:userName) ";

    public static final String GET_POPULAR_TAG = "getPopularTag";

    public static final String GET_POPULAR_TAG_QUERY = "SELECT COUNT(tg.id) FROM Tagging tg "
            + "WHERE tg.tag.id=:tagId AND tg.targetDocument.id IN (:listOfIds) AND "
            + "(tg.isPrivate=false OR tg.author=:userName) ";

    public static final String REMOVE_TAGGING = "removeTagging";

    public static final String REMOVE_TAGGING_QUERY = "DELETE FROM Tagging tagging WHERE tagging.targetDocument.id=:targetId AND "
            + "tagging.author=:userName AND tagging.tag.id=:tagId";

    public static final String GET_VOTE_CLOUD = "getVoteCloud";

    public static final String GET_VOTE_CLOUD_QUERY = "SELECT tag.id, tag.label, COUNT(tag.id) FROM Tagging tg "
            + "JOIN tg.tag tag JOIN tag.hierarchy h JOIN h.dublincore dc "
            + "WHERE tg.targetDocument.id IN (:listOfIds) AND "
            + "(tg.isPrivate=false OR tg.author=:userName) AND "
            + "(tg.tag.private1 = false OR dc.creator = :userName) GROUP BY tag.id , tag.label";

    /*Not used anymore, remains as informative chunk*/
    public static final String GET_POPULAR_CLOUD = "getPopularCloud";

    /*Not used anymore, remains as informative chunk*/
    public static final String GET_POPULAR_CLOUD_QUERY = "SELECT tag.id, tag.label, COUNT(DISTINCT tg.targetDocument.id) FROM Tagging tg "
            + "JOIN tg.tag tag JOIN tag.hierarchy h JOIN h.dublincore dc "
            + "WHERE tg.targetDocument.id IN (:listOfIds) AND "
            + "(tg.isPrivate=false OR tg.author=:userName) AND "
            + "(tg.tag.private1 = false OR dc.creator = :userName) GROUP BY tag.id , tag.label";

    private TaggingConstants() {
    }
}
