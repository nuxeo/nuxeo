This module figures out how using the switch user feature provided by the web nuxeo authentication filter.

Users may mandate other users for connecting to the application using their rights (>Deputies/My deputies).
Users may use their mandates and re-connect themselves using the mandated permissions (>Deputies/My mandates).

Technically, the reconnection is managed by redirecting the client to the switch user page.
The mandated user login is transfered to the serer using an attribute
If the user login attribute is not specified, and the user is already using a mandate, he's
reconnected back using his original identity.
Note that we're also disabling the automatic redirect feature used by the URL rewriting part of nuxeo.

...
            String targetURL = "/" + NXAuthConstants.SWITCH_USER_PAGE;

            request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
                    true);
            if (login != null) {
                request.setAttribute(NXAuthConstants.SWITCH_USER_KEY, login);
            }
...
