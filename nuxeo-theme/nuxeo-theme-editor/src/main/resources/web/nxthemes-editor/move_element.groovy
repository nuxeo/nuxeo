
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager

src_id = Request.getParameter("src_id")
dest_id = Request.getParameter("dest_id")
order = new Integer(Request.getParameter("order"))

Element srcElement = ThemeManager.getElementById(src_id);
Element destElement = ThemeManager.getElementById(dest_id);

// move the element
srcElement.moveTo(destElement, order);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(srcElement, destElement));

