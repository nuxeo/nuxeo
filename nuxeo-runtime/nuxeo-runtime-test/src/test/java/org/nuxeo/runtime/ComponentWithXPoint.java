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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/** @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> */
public class ComponentWithXPoint extends DefaultComponent {

    private static final Log log = LogFactory.getLog(ComponentWithXPoint.class);

    public static final ComponentName NAME = new ComponentName("BaseXPoint");

    final List<DummyContribution> contribs = new ArrayList<DummyContribution>();

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            log.debug("Registering: " + ((DummyContribution) contrib).message);
            this.contribs.add((DummyContribution) contrib);
        }
    }

    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            log.debug("Un-Registering: " + ((DummyContribution) contrib).message);
            this.contribs.add((DummyContribution) contrib);
        }
    }

    public DummyContribution[] getContributions() {
        return contribs.toArray(new DummyContribution[contribs.size()]);
    }

}
