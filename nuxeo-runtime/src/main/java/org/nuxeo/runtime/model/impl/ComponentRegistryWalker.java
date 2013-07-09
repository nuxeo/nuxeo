package org.nuxeo.runtime.model.impl;

import java.util.HashSet;
import java.util.Set;

public class ComponentRegistryWalker {

    protected final ComponentRegistry registry;

    public ComponentRegistryWalker(ComponentRegistry registry) {
        this.registry = registry;
    }

    public interface Visitor {
        void visit(RegistrationInfoImpl info, int depth, boolean expanded);
    }

    protected static class StringVisitor implements Visitor {

        protected final StringBuilder builder;

        public StringVisitor(StringBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void visit(RegistrationInfoImpl info, int depth, boolean expanded) {
            for (int i = 0; i < depth; ++i) {
                builder.append(' ');
            }
            builder.append(info + "," + depth + "," + expanded + "\n");
        }

    }

    protected static class Context {
        final Set<RegistrationInfoImpl> visited = new HashSet<RegistrationInfoImpl>();
    }

    public void walk(Visitor visitor) {
        Context context = new Context();
        for (RegistrationInfoImpl info : registry.components.values()) {
            if (info.requires.isEmpty()) {
                doWalk(context, info, visitor);
            }
        }

    }

    protected int doWalk(Context context, RegistrationInfoImpl info,
            Visitor visitor) {
        int depth = 0;
        if (!context.visited.add(info)) {
            for (RegistrationInfoImpl other : info.dependsOnMe) {
                int otherdepth = doWalk(context, other, visitor) + 1;
                if (otherdepth > depth) {
                    depth = otherdepth;
                }
            }
        }
        visitor.visit(info, depth, true);
        return depth;
    }

    public void append(StringBuilder msg) {
        walk(new StringVisitor(msg));
    }

}
