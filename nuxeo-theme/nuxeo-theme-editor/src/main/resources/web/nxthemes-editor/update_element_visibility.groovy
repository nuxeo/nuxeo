import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.perspectives.PerspectiveManager

id = Request.getParameter("id")
perspectives = Request.getParameterValues("perspectives")
alwaysVisible = Request.getParameter("always_visible").equals("true") ? true : false

List<String> perspectivesList = new ArrayList<String>()
if (perspectives != null) {
    for (p in perspectives) {
        perspectivesList.add(p)
    }
}

Element element = ThemeManager.getElementById(id)
PerspectiveManager perspectiveManager = Manager.getPerspectiveManager()

if (alwaysVisible) {
    perspectiveManager.setAlwaysVisible(element)
} else {
    // initially make the element visible in all perspectives
    if (perspectivesList.isEmpty()) {
        perspectiveManager.setVisibleInAllPerspectives(element)
    } else {
        perspectiveManager.setVisibleInPerspectives(element, perspectivesList)
    }
}

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
