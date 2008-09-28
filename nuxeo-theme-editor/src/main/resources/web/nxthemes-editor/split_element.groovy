
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.formats.FormatFactory

id = Request.getParameter("id")

Element element = ThemeManager.getElementById(id)
if (!element.getElementType().getTypeName().equals("cell")) {
    return
}
Element newCell = ElementFactory.create("cell")
Format cellWidget = FormatFactory.create("widget")
cellWidget.setName("cell frame")
themeManager.registerFormat(cellWidget)
Format cellLayout = FormatFactory.create("layout")
themeManager.registerFormat(cellLayout)
FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT, "layout")

Format layout = ElementFormatter.getFormatByType(element, layoutType)
width = layout.getProperty("width")
if (width != null) {
    halfWidth = Utils.divideWebLength(width, 2)
    if (halfWidth != null) {
        cellLayout.setProperty("width", halfWidth)
        layout.setProperty("width", Utils.substractWebLengths(width, halfWidth))
    }
}

Format cellStyle = FormatFactory.create("style")
themeManager.registerFormat(cellStyle)
ElementFormatter.setFormat(newCell, cellWidget)
ElementFormatter.setFormat(newCell, cellLayout)
ElementFormatter.setFormat(newCell, cellStyle)
newCell.insertAfter(element)

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
