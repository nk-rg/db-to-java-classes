package com.dico.gen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Entity {
  private String entityTableName;
  private String tableName;
  private List<Field> fields;
  private List<Entity> entitiesUsingThis = new ArrayList<>();
  private List<ChildReference> childReferences = new ArrayList<>();

  public String getEntityTableName() {
    return entityTableName;
  }

  public void setEntityTableName(String entityTableName) {
    this.entityTableName = entityTableName;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public String getJavaImports() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    List<String> imports = new ArrayList<>();
    fields.forEach(field -> {
      if (!imports.contains("LocalDate") && field.getType().equals("LocalDate")) {
        imports.add("import java.time.LocalDate;");
      }
      if (!imports.contains("LocalDateTime") && field.getType().equals("LocalDateTime")) {
        imports.add("import java.time.LocalDateTime;");
      }
      if (!imports.contains("BigDecimal") && field.getType().equals("BigDecimal")) {
        imports.add("import java.math.BigDecimal;");
      }
      if (!imports.contains("Duration") && field.getType().equals("Duration")) {
        imports.add("import java.time.Duration;");
      }
    });
    Collections.sort(imports);
    return String.join("\n", imports);
  }

  public boolean anyFieldIsType(String type) {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().anyMatch(field -> field.getType().equals(type));
  }

  public boolean hasComposedId() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().filter(Field::isPrimaryKey).count() > 1;
  }

  public boolean hasEnum() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().anyMatch(Field::isEnumType);
  }

  public boolean hasForeignKey() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().filter(Field::isForeignKey).count() > 1;
  }

  public List<Field> getComposedFields() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().filter(Field::isPrimaryKey).collect(Collectors.toList());
  }

  public List<Entity> getEntitiesUsingThis() {
    return entitiesUsingThis;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public boolean hasOnePrimaryKey() {
    return fields.stream().filter(field -> field.isPrimaryKey() && !field.isForeignKey()).count() == 1;
  }

  public boolean hasOneToMany() {
    return !entitiesUsingThis.isEmpty();
  }

  public boolean hasManyToOne() {
    return fields.stream().anyMatch(field -> !field.isPrimaryKey() && field.isForeignKey());
  }

  public List<ChildReference> getChildReferences() {
    return childReferences;
  }

  public void setChildReferences(List<ChildReference> childReferences) {
    this.childReferences = childReferences;
  }

  public boolean hasOneToOneSharedPrimaryKey() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return !hasComposedId() && fields.stream().anyMatch(field -> field.isPrimaryKey() && field.isForeignKey());
  }

  public boolean hasUniques() {
    if (fields == null) {
      throw new IllegalStateException("No existen campos para esta tabla");
    }
    return fields.stream().anyMatch(Field::isUnique);
  }

  @Override
  public String toString() {
    return "Entity{" +
            "entityTableName='" + entityTableName + '\'' +
            ", fields=\n\t" + fields.stream().map(Field::toString).collect(Collectors.joining("\n\t")) +
            ", childReferences="+childReferences.stream().map(ChildReference::toString).toList() +
            '}';
  }

  //  @Override
//  public String toString() {
//    return "Entity{" +
//      "entityTableName='" + entityTableName + '\'' +
//      ", fields=\n\t" + fields.stream().map(Field::toString).collect(Collectors.joining("\n\t")) +
//      '}';
//  }
}
