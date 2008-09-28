
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element

id = Request.getParameter("id")
description = Request.getParameter("description")

Element element = ThemeManager.getElementById(id)
element.setDescription(description)

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null))
