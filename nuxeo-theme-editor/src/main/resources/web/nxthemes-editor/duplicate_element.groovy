
import org.nuxeo.theme.Manager
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager
import org.nuxeo.theme.elements.Element

id = Request.getParameter("id");

Element element = ThemeManager.getElementById(id);
Element duplicate = Manager.getThemeManager().duplicateElement(element, true);

// insert the duplicated element
element.getParent().addChild(duplicate);
duplicate.moveTo(element.getParent(), element.getOrder() + 1);

EventManager eventManager = Manager.getEventManager();
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(null, null));
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, element));

return duplicate.getUid().toString();
