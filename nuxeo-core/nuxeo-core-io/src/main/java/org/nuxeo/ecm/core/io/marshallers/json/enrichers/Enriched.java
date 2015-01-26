package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

public class Enriched<EntityType> {

    private EntityType enrichable;

    public Enriched(EntityType enrichable) {
        super();
        this.enrichable = enrichable;
    }

    public EntityType getEntity() {
        return enrichable;
    }

}
