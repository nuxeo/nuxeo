/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.rpc;

/**
 * @author Stéphane Fourrier
 */
public class InitApplication extends AbstractAction<InitApplicationResult> {

    private static final long serialVersionUID = 1L;

    private String spaceProviderName;

    private String spaceName;

    @SuppressWarnings("unused")
    private InitApplication() {
        super();
    }

    public InitApplication(ContainerContext containerContext,
            String spaceProviderName, String spaceName) {
        super(containerContext);
        this.spaceProviderName = spaceProviderName;
        this.spaceName = spaceName;
    }

    public String getSpaceProviderName() {
        return spaceProviderName;
    }

    public String getSpaceName() {
        return spaceName;
    }

}
