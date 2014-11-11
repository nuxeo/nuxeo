import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager

srcId = Request.getParameter("src_id")
destId = Request.getParameter("dest_id")
order = new Integer(Request.getParameter("order"))

Element srcElement = ThemeManager.getElementById(src_I);
Element destElement = ThemeManager.getElementById(destId);

// move the element
srcElement.moveTo(destElement, order);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(srcElement, destElement));

