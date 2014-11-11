/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.tag;

/**
 * Aggregates the weight with the tag holder. Note that usually the tag cloud
 * is generated considering the public/private flags.
 * <p>
 * We can basically define 2 types of clouds: "vote" and "popularity". The first
 * is counting how many times a tag was applied on a document by different
 * users (votes), while the second counts how many documents in a particular
 * domain were tagged with a particular tag, aiming to measure the tag
 * popularity in a domain.
 * <p>
 * These tag cloud definitions I took from Wikipedia:
 * http://en.wikipedia.org/wiki/Tag_cloud . Maybe not the best source though.
 * <p>
 * Let's have an example:
 * <p>
 * - have domain WorkspaceA with 2 documents Doc1 and Doc2. The tag tagX is
 * applied by 3 different users on Doc1, tagY is applied by 5 different users
 * on Doc2, tagZ is applied once on Doc1 and once on Doc2. Also, tagX was
 * applied twice on WorkspaceA. The tag clouds would be:
 * <ul>
 * <li>"vote" on Doc1: tagX - 3, tagZ - 1
 * <li>"popularity" on Doc1: tagX - 1, tagZ - 1
 * <li>"vote" on Doc2: tagY - 5, tagZ - 1
 * <li>"popularity" on Doc2: tagY - 1, tagZ - 1
 * <li>"vote" on WorkspaceA: tagX - 2
 * <li>"popularity" on WorkspaceA: tagX - 2, tagZ - 2, tagY - 1
 * </ul>
 * There is a third less used tag cloud: the number of times the tag appears in
 * the content of an item. This would be harder to implement (the content needs
 * to be interpreted) and apparently less used. Indeed, to apply a tag like
 * "interesting", or "misleading" don't need that these terms appear in the
 * article.
 *
 * @author rux
 */
public class WeightedTag extends Tag {

    private static final long serialVersionUID = 8142899916461750143L;

    /**
     * The weight of the tag. Could be vote or popular weight, depending of the
     * caller request.
     */
    public int weight;

    public WeightedTag(String tagId, String tagLabel, int weight) {
        super(tagId, tagLabel);
        this.weight = weight;
    }

    public WeightedTag() {
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

}
