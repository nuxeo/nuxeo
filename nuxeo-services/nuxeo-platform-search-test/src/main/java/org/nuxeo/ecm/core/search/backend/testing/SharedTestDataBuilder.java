/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.core.search.backend.testing;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedDataImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourceImpl;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourcesImpl;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;

/**
 * A class to share creation of testing data across tests. The data is supposed
 * to be books, but that could change.
 * <p>
 * Should probably go to org.nuxeo.com.search.api
 *
 * @author gracinet@nuxeo.com
 *
 */
public class SharedTestDataBuilder {

    // Constant utility class.
    private SharedTestDataBuilder() {
    }

    public static ResolvedResource aboutLifeIndexableBookSchemaResource() {
        return bookDataModel("bk_about_life", // id
                "La méchante vie de l'auteur", // French title
                "0000", // barcode
                "autobio", // category
                "Contents should not be stored", // content
                "Abstracts aren't indexed but stored", // abstract
                Arrays.asList("philosophy", "people"), // tags
                null, // published
                437 // pages
        );
    }

    /** Same with different barcode **/
    public static ResolvedResource aboutLifeIndexableBookSchemaResource2() {
        return bookDataModel("bk_about_life", // id
                "La méchante vie de l'auteur", // French title
                "EXT0000_1", // barcode
                "autobio", // category
                "Contents should not be stored", // content
                "Abstracts aren't indexed but stored", // abstract
                Arrays.asList("philosophy", "people"), // tags
                null, // published
                437 // pages
        );
    }

    public static ACP makeAboutLifeACP() {
        ACL acl = new ACLImpl();
        // TODO add something about groups of permissions somewhere

        acl.add(new ACE("dupont", SecurityConstants.BROWSE, true));
        acl.add(new ACE("dupont", "Count", false));
        acl.add(new ACE("hugo", SecurityConstants.BROWSE, false));
        acl.add(new ACE("authors", SecurityConstants.BROWSE, true));
        acl.add(new ACE("sales", SecurityConstants.BROWSE, false));

        ACP acp = new ACPImpl();
        acp.addACL(0, acl);

        acl = new ACLImpl();
        acl.add(new ACE("accountants", "Count", true));
        acl.add(new ACE("employees", SecurityConstants.BROWSE, true));
        acp.addACL(1, acl);
        return acp;
    }

    public static ACP makeWarPeaceACP() {
        ACL acl = new ACLImpl();
        // TODO add something about groups of permissions somewhere

        acl.add(new ACE("goethe", SecurityConstants.BROWSE, true));
        acl.add(new ACE("dupont", SecurityConstants.BROWSE, false));
        acl.add(new ACE("durand", SecurityConstants.EVERYTHING, false));
        acl.add(new ACE("hugo", SecurityConstants.READ, true));
        acl.add(new ACE("tolstoi", SecurityConstants.READ, false));

        ACP acp = new ACPImpl();
        acp.addACL(0, acl);

        acl = new ACLImpl();
        acl.add(new ACE("authors", SecurityConstants.READ, true));
        acl.add(new ACE("admins", SecurityConstants.EVERYTHING, true));
        acp.addACL(1, acl);
        return acp;
    }

    public static List<ResolvedData> aboutLifeCommon() {
        return commonDataResource("builtin_about_life",
                new PathRef("some/path"), // Doc Ref),
                "some/path", // path
                "about life full text", true);
    }

    public static ResolvedResources makeAboutLifeAggregated() {
        return new ResolvedResourcesImpl("agg_id",
                Arrays.asList(
                        aboutLifeIndexableBookSchemaResource(),
                        dCDataModel("dc_about_life", "About Life")),
                        aboutLifeCommon(),
                makeAboutLifeACP());
    }

    public static ResolvedResources makeAboutLifeAggregated2() {
        return new ResolvedResourcesImpl("agg_id",
                Arrays.asList(
                        aboutLifeIndexableBookSchemaResource2(),
                        dCDataModel("dc_about_life", "About Life")),
                        aboutLifeCommon(),
                makeAboutLifeACP());
    }

    @SuppressWarnings("unchecked")
    public static ResolvedResources makeWarPeace() {
        String baseId = "war_peace";
        return new ResolvedResourcesImpl("TLST",
                Arrays.asList(
                        bookDataModel(
                                "book_" + baseId, // atomic id
                                "La Guerre et la paix", // French title G uppercase
                                "0018", // barcode
                                "novel", // category
                                "A very very long string :-)", // content
                                "Famous novel", // abstract
                                Collections.<String>emptyList(), // tags
                                null, // published
                                1789  // pages
                        ),
                        dCDataModel("dc_" + baseId,
                                "War and Peace") // title
                ),
                commonDataResource("builtin_" + baseId,
                        new IdRef("war-peace"), // docRef
                        "russian/warpeace", // path
                        "War and peace Full Text", false),
                makeWarPeaceACP());
    }

    /**
     * Makes aggregated resources for a bunch of almost identical books.
     * <p>
     * Barcode pattern: 135xxxx
     * <p>
     * Resource id pattern: RVLxxxx
     *
     * @param nb
     *            the number of resources to create
     * @return the resoures in an array
     */
    public static ResolvedResources[] revelationsBunch(int nb) {
        ResolvedResources[] resources = new ResolvedResourcesImpl[nb];
        for (int i = 0; i < nb; i++) {
            resources[i] = revelations(i);
        }
        return resources;
    }

    /**
     * Makes aggregated resource for a book ("Revelations") with an integer
     * parameter.
     * <p>
     * Barcode pattern: 135xxxx
     * <p>
     * Resource id pattern: RVLxxxx
     * <p>
     * published date: April <pre>i</pre>-th, 2007
     *
     * @param i the integer parameter
     * @return the resoures in an array
     */
    public static ResolvedResources revelations(int i) {
        Calendar calValue = Calendar.getInstance();
        calValue.clear();
        calValue.set(2007, 3, i + 1, 3, 57);

        String baseId = String.format("rvl%04d", i);  // id

        return new ResolvedResourcesImpl(
            String.format("RVL%04d", i),
            Arrays.asList(
                    bookDataModel(
                            "bk_" + baseId, // id
                            "Révélations", // French title
                            String.format("135%04d", i), // barcode
                            "novel", // category
                            "Some real info about real people", // content
                            String.format("Revelations, %d edition", i), // abstract
                            Arrays.asList("gossip", "people"), // tags
                            calValue, // published
                            100 - i // pages
                    ),
                    dCDataModel("dc_" + baseId, // id
                            "Revelations" // title
                    )
            ),
            commonDataResource(
                    "builtin_" + baseId, // id
                    new IdRef(String.format("id_rvl%04d", i)), // docRef
                    String.format("some/rev/%d", i), // path
                    "Revelations full text", false
            ),
            null);
    }

    public static ResolvedResources revelationsEmptyDate(int i) {

        String baseId = String.format("rvl%04d", i);  // id

        return new ResolvedResourcesImpl(
            String.format("RVL%04d", i),
            Arrays.asList(
                    bookDataModel(
                            "bk_" + baseId, // id
                            "Révélations", // French title
                            String.format("135%04d", i), // barcode
                            "novel", // category
                            "Some real info about real people", // content
                            String.format("Revelations, %d edition", i), // abstract
                            Arrays.asList("gossip", "people"), // tags
                            null, // published
                            100 - i // pages
                    ),
                    dCDataModel("dc_" + baseId, // id
                            "Revelations" // title
                    )
            ),
            commonDataResource(
                    "builtin_" + baseId, // id
                    new IdRef(String.format("id_rvl%04d", i)), // docRef
                    String.format("some/rev/%d", i), // path
                    "Revelations full text", false
            ),
            null);
    }

    /*
     *  Internals
     *
     */

    private static IndexableResource resourceProxy(String name, String prefix) {
        return new FakeIndexableResource(
                new FakeIndexableDocResourceConf(name, prefix));
    }

    private static ResolvedResource bookDataModel(String id,
            String frenchTitle, String barcode, String category,
            String contents, String abst, List<String> tags, Calendar calValue, int pages) {

        List<ResolvedData> iData = new LinkedList<ResolvedData>();
        iData.add(new ResolvedDataImpl("frenchtitle", "french", "Text",
                frenchTitle, true, true, false, true, "case-insensitive",
                null, false, null));
        iData.add(new ResolvedDataImpl("barcode", null, "Keyword", barcode,
                true, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl("published", null, "Date", calValue,
                true, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl("category", null, "Keyword", category,
                true, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl("contents", "anal", "Text", contents,
                false, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl("abstract", "anal", "Text", abst, true,
                false, false, false, null, null, false, null));
        String[] tagsArray = new String[tags.size()];
        int i = 0;
        for (String tag : tags) {
            tagsArray[i++] = tag;
        }
        iData.add(new ResolvedDataImpl("tags", "anal", "Keyword", tagsArray, true,
                true, true, false, null, null, false, null));
        iData.add(new ResolvedDataImpl("pages", null, "Int", pages, true,
                true, false, false, null, null, false, null));

        return new ResolvedResourceImpl(id, resourceProxy("book", "bk"), iData);
    }

    private static List<ResolvedData> commonDataResource(String id,
            DocumentRef docRef, String path, String fulltext, boolean isVersion) {

        List<ResolvedData> iData = new LinkedList<ResolvedData>();
        iData.add(new ResolvedDataImpl(BuiltinDocumentFields.FIELD_DOC_REF,
                null, "Builtin", docRef, // builtin type should avoid
                // tokenization and trigger serialization
                true, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl(BuiltinDocumentFields.FIELD_DOC_PATH,
                null, "Path", new Path(path), true, true, false, false,
                null, null, false, null));
        iData.add(new ResolvedDataImpl(BuiltinDocumentFields.FIELD_DOC_TYPE,
                null, "Keyword", "Book", true, true, false, false,
                null, null, false, null));
        iData.add(new ResolvedDataImpl(
                BuiltinDocumentFields.FIELD_FULLTEXT,
                null, "text", fulltext, true, true, false, false, null, null, false, null));
        iData.add(new ResolvedDataImpl(
                BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE,
                null, "Builtin", "project", true, true, false, false,
                null, null, false, null));
        iData.add(new ResolvedDataImpl(
                BuiltinDocumentFields.FIELD_DOC_IS_CHECKED_IN_VERSION,
                null, "Boolean", isVersion, true, true, false, false,
                null, null, false, null));
      return iData;
    }

    private static ResolvedResource dCDataModel(String id,  String title) {

        List<ResolvedData> iData = new LinkedList<ResolvedData>();
        iData.add(new ResolvedDataImpl("title", "standard", "Text", title,
                true, true, false, true, null, null, false, null));
        return new ResolvedResourceImpl(id,
                resourceProxy("dublincore", "dc"), iData);
    }

}
