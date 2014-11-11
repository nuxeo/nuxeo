import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.perspectives.PerspectiveManager

id = Request.getParameter("id")
perspectives = Request.getParameter("perspectives")
always_visible = Request.getParameter("always_visible")

Element element = ThemeManager.getElementById(id)
PerspectiveManager perspectiveManager = Manager.getPerspectiveManager()

if (always_visible) {
    perspectiveManager.setAlwaysVisible(element)
} else {
    // initially make the element visible in all perspectives
    if (perspectives.isEmpty()) {
        perspectiveManager.setVisibleInAllPerspectives(element)
    } else {
        perspectiveManager.setVisibleInPerspectives(element, perspectives)
    }
}

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
