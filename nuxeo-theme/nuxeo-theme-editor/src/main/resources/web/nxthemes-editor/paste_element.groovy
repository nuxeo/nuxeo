import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element

destId = Request.getParameter("dest_id")
id = Request.getSession(true).getAttribute("nxthemes.editor.clipboard")

if (id == null) {
    return
}

Element destElement = ThemeManager.getElementById(destId)
if (destElement.isLeaf()) {
    destElement = (Element) destElement.getParent()
}

Element element = ThemeManager.getElementById(id)
if (element != null) {
    destElement.addChild(Manager.getThemeManager().duplicateElement(element, true))
}

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null))
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, destElement))
