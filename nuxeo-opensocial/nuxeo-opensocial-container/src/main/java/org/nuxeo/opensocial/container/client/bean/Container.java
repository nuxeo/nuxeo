/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client.bean;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Global container of gadgets
 *
 * @author Guillaume Cusnieux
 *
 */
public class Container implements IsSerializable {

    private static final long serialVersionUID = 1L;

    private static final String LAYOUT_SEPARATOR = "-";

    private static final String LAYOUT_MAXGADGETS_SEPARATOR = "-max-";

    private List<GadgetBean> gadgets;

    private List<String> permissions;

    private String layout;

    private String spaceId;

    private int structure;

    private int[] maxGadgets;

    /**
     * Default construcor (Specification of Gwt)
     */
    public Container() {

    }

    /**
     * Constructor for create Container instance with all important parameter
     *
     * @param gadgets
     * @param structure
     * @param permissions
     */
    public Container(List<GadgetBean> gadgets, int structure, String layout,
            List<String> permissions, String spaceId) {
        this.gadgets = gadgets;
        this.layout = layout;
        this.structure = structure;
        this.permissions = permissions;
        this.spaceId = spaceId;
        computeMaxGadgets();
    }

    public List<GadgetBean> getGadgets() {
        return this.gadgets;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getLayout() {
        return layout;
    }

    public int getStructure() {
        return structure;
    }

    public void setLayout(String layout) {
        this.layout = layout;
        computeMaxGadgets();
    }

    public void setStructure(int structure) {
        this.structure = structure;
    }

    public List<String> getPermission() {
        return permissions;
    }

    public GadgetBean getGadgetBean(String ref) {
        for (GadgetBean bean : gadgets) {
            if (ref.equals(bean.getRef()))
                return bean;
        }
        return null;
    }

    protected void computeMaxGadgets() {
        if (layout.contains(LAYOUT_MAXGADGETS_SEPARATOR)) {
            maxGadgets = new int[structure];
            Arrays.fill(maxGadgets, -1);
            String[] maxAsStr = layout.split(LAYOUT_MAXGADGETS_SEPARATOR)[1].split(LAYOUT_SEPARATOR);
            for (int i = 0; i < maxAsStr.length; i++) {
                maxGadgets[i] = Integer.parseInt(maxAsStr[i]);
            }
        }
    }

    public int[] getMaxGadgets() {
        return maxGadgets;
    }

}
