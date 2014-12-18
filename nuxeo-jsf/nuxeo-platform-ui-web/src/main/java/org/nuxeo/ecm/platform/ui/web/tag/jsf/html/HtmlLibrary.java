/**
 * Licensed under the Common Development and Distribution License,
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.sun.com/cddl/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 *
 * $Id: HtmlLibrary.java 24932 2007-09-13 16:32:19Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.tag.jsf.html;

import org.nuxeo.ecm.platform.ui.web.component.message.NXMessagesRenderer;
import org.nuxeo.ecm.platform.ui.web.renderer.NXCheckboxRenderer;
import org.nuxeo.ecm.platform.ui.web.renderer.NXImageRenderer;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.MetaActionSourceTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.MetaValueHolderTagHandler;

import com.sun.faces.facelets.tag.jsf.html.AbstractHtmlLibrary;

/**
 * Replicate the HTML Library with facelet handlers to use a specific namespace.
 *
 * @author Jacob Hookom
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class HtmlLibrary extends AbstractHtmlLibrary {

    public static final String Namespace = "http://nuxeo.org/nxweb/html";

    public static final HtmlLibrary Instance = new HtmlLibrary();

    public HtmlLibrary() {
        super(Namespace);

        addHtmlComponent("column", "javax.faces.Column", null);

        this.addComponent("commandButton", "javax.faces.HtmlCommandButton", "javax.faces.Button",
                MetaActionSourceTagHandler.class);

        this.addComponent("commandLink", "javax.faces.HtmlCommandLink", "javax.faces.Link",
                MetaActionSourceTagHandler.class);

        addHtmlComponent("dataTable", "javax.faces.HtmlDataTable", "javax.faces.Table");

        addHtmlComponent("form", "javax.faces.HtmlForm", "javax.faces.Form");

        addHtmlComponent("graphicImage", "javax.faces.HtmlGraphicImage", NXImageRenderer.RENDERER_TYPE);

        addHtmlComponent("inputHidden", "javax.faces.HtmlInputHidden", "javax.faces.Hidden");

        addHtmlComponent("inputSecret", "javax.faces.HtmlInputSecret", "javax.faces.Secret");

        addHtmlComponent("inputText", "javax.faces.HtmlInputText", "javax.faces.Text");

        addHtmlComponent("inputTextarea", "javax.faces.HtmlInputTextarea", "javax.faces.Textarea");

        addHtmlComponent("message", "javax.faces.HtmlMessage", "javax.faces.Message");

        addHtmlComponent("messages", "javax.faces.HtmlMessages", NXMessagesRenderer.RENDERER_TYPE);

        addHtmlComponent("outputFormat", "javax.faces.HtmlOutputFormat", "javax.faces.Format");

        addHtmlComponent("outputLabel", "javax.faces.HtmlOutputLabel", "javax.faces.Label");

        addHtmlComponent("outputLink", "javax.faces.HtmlOutputLink", "javax.faces.Link");

        // meta value wired
        this.addComponent("metaOutputLink", "javax.faces.HtmlOutputLink", "javax.faces.Link",
                MetaValueHolderTagHandler.class);

        this.addComponent("outputText", "javax.faces.HtmlOutputText", "javax.faces.Text",
                MetaValueHolderTagHandler.class);

        addHtmlComponent("panelGrid", "javax.faces.HtmlPanelGrid", "javax.faces.Grid");

        addHtmlComponent("panelGroup", "javax.faces.HtmlPanelGroup", "javax.faces.Group");

        addHtmlComponent("selectBooleanCheckbox", "javax.faces.HtmlSelectBooleanCheckbox",
                NXCheckboxRenderer.RENDERER_TYPE);

        addHtmlComponent("selectManyCheckbox", "javax.faces.HtmlSelectManyCheckbox", "javax.faces.Checkbox");

        addHtmlComponent("selectManyListbox", "javax.faces.HtmlSelectManyListbox", "javax.faces.Listbox");

        addHtmlComponent("selectManyMenu", "javax.faces.HtmlSelectManyMenu", "javax.faces.Menu");

        addHtmlComponent("selectOneListbox", "javax.faces.HtmlSelectOneListbox", "javax.faces.Listbox");

        addHtmlComponent("selectOneMenu", "javax.faces.HtmlSelectOneMenu", "javax.faces.Menu");

        addHtmlComponent("selectOneRadio", "javax.faces.HtmlSelectOneRadio", "javax.faces.Radio");
    }

    @Override
    public void addHtmlComponent(String name, String componentType, String rendererType) {
        super.addComponent(name, componentType, rendererType, GenericHtmlComponentHandler.class);
    }

}
