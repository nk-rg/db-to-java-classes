package com.dico.gen.model;

public class ChildReference {

    private Entity entity;
    private Field field;

    public ChildReference(Entity entity, Field field) {
        this.entity = entity;
        this.field = field;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
