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

    private List<GadgetBean> gadgets;

    private String layout;

    private String spaceId;

    private int structure;

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
    public Container(List<GadgetBean> gadgets, int structure, String layout, String spaceId) {
        this.gadgets = gadgets;
        this.layout = layout;
        this.structure = structure;
        this.spaceId = spaceId;
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
    }

    public void setStructure(int structure) {
        this.structure = structure;
    }

    public GadgetBean getGadgetBean(String ref) {
        for (GadgetBean bean : gadgets) {
            if (ref.equals(bean.getRef()))
                return bean;
        }
        return null;
    }

}
