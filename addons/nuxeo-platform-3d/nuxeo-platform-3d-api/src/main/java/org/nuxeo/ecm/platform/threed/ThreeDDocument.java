/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed;

import java.util.Collection;

/**
 * Interface for document adapter wrapping a 3D document. Gives access to the transmission formats in the underlying
 * document.
 *
 * @since 8.4
 */
public interface ThreeDDocument {

    /**
     * Returns the main {@link ThreeD} of this {@code ThreeDDocument}.
     */
    ThreeD getThreeD();

    /**
     * Returns all the {@link TransmissionThreeD}s for this {@code ThreeDDocument}.
     */
    Collection<TransmissionThreeD> getTransmissionThreeDs();

    /**
     * Returns a given {@link TransmissionThreeD} based on its {@code name}, {@code null} if this transmission 3d does
     * not exist.
     */
    TransmissionThreeD getTransmissionThreeD(String name);

    /**
     * Returns all the {@link ThreeDRenderView}s for this {@code ThreeDDocument}.
     */
    Collection<ThreeDRenderView> getRenderViews();

    /**
     * Returns a given {@link ThreeDRenderView} based on its {@code title}, {@code null} if this render view does not
     * exist.
     */
    ThreeDRenderView getRenderView(String title);

}
