package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * XMap descriptor for registering overidable parameterized Suggester
 * implementation on the SuggesterService.
 * 
 * @author ogrisel
 */
public class SuggesterDescriptor implements Cloneable {

    protected String name = "default";

    protected String className;

    protected boolean enabled = true;

    protected Map<String, String> parameters = new HashMap<String, String>();

    protected Suggester suggester;

    protected RuntimeContext runtimeContext;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setRuntimeContext(RuntimeContext context)
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        // store the runtime context for later usage if a merge is required
        this.runtimeContext = context;
        loadParameterizedSuggester();
    }

    protected void loadParameterizedSuggester() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        if (enabled && className != null) {
            // try build the suggester instance as early as possible to throw
            // errors at deployment time rather than lazily at first access time
            // by the user: fail early.
            suggester = (Suggester) runtimeContext.loadClass(className).newInstance();
            suggester.setParameters(parameters);
        }
        // if the the descriptor is enabled but does not provide any
        // contrib this is probably just for overriding some parameters
        // handled at merge time
    }

    public Suggester getSuggester() {
        return suggester;
    }

    public void mergeFrom(SuggesterDescriptor previousDescriptor)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        if (name == null || !name.equals(previousDescriptor)) {
            throw new RuntimeException("Cannot merge descriptor with name '"
                    + name + "' with another descriptor with different name "
                    + previousDescriptor.getName() + "'");
        }
        if (className == null) {
            if (enabled && previousDescriptor.className == null) {
                throw new RuntimeException(
                        "Cannot merge descriptor with name '" + name
                                + "' with source a source version that has no"
                                + " className defined.");
            }
            className = previousDescriptor.className;
            runtimeContext = previousDescriptor.runtimeContext;
        }
        // merged the parameters
        Map<String, String> mergedParameters = new HashMap<String, String>();
        mergedParameters.putAll(previousDescriptor.parameters);
        mergedParameters.putAll(parameters);
        parameters = mergedParameters;
        loadParameterizedSuggester();
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
