        
import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.events.EventManager

EventManager eventManager = Manager.getEventManager();

eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null, null));
return "";
