import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.html.Utils

Style selectedStyleLayer = Context.runScript("getSelectedStyleLayer.groovy")

id = Request.getParameter("id")
Element element = ThemeManager.getElementById(id)

FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "style")
Style style = (Style) ElementFormatter.getFormatByType(element, styleType)
if (selectedStyleLayer != null) {
    style = selectedStyleLayer
}

if (style.getName() != null || "".equals(viewName)) {
    viewName = "*"
}

Utils.loadCss(style, cssSource, viewName)

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null))
