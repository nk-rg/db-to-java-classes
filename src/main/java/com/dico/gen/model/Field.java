package com.dico.gen.model;

import java.util.List;

public class Field {
  private String fieldName;
  private String columnName;
  private String type;
  private int size;
  private int scale;
  private boolean primaryKey;
  private boolean foreignKey;
  private boolean isUnique;
  private boolean enumType;
  private String columnDefinition;
  private List<String> valuesOfEnum;
  private String foreignTableName;
  private String foreignColumnName;

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getScale() {
    return scale;
  }

  public void setScale(int scale) {
    this.scale = scale;
  }

  public void setPrimaryKey(boolean primaryKey) {
    this.primaryKey = primaryKey;
  }

  public boolean isForeignKey() {
    return foreignKey;
  }

  public void setForeignKey(boolean foreignKey) {
    this.foreignKey = foreignKey;
  }

  public boolean isEnumType() {
    return enumType;
  }

  public void setEnumType(boolean enumType) {
    this.enumType = enumType;
  }

  public List<String> getValuesOfEnum() {
    return valuesOfEnum;
  }

  public void setValuesOfEnum(List<String> valuesOfEnum) {
    this.valuesOfEnum = valuesOfEnum;
  }

  public boolean isPrimaryKey() {
    return primaryKey;
  }

  public boolean isUnique() {
    return isUnique;
  }

  public void setUnique(boolean unique) {
    isUnique = unique;
  }

  public void setForeignTableName(String foreignTableName) {
    this.foreignTableName = foreignTableName;
  }

  public void setForeignColumnName(String foreignColumnName) {
    this.foreignColumnName = foreignColumnName;
  }

  public String getForeignTableName() {
    return foreignTableName;
  }

  public String getForeignColumnName() {
    return foreignColumnName;
  }

  public String getColumnDefinition() {
    return columnDefinition;
  }

  public void setColumnDefinition(String columnDefinition) {
    this.columnDefinition = columnDefinition;
  }

  @Override
  public String toString() {
    return "Field{" +
      "fieldName='" + fieldName + '\'' +
      ", type='" + type + '\'' +
      ", size=" + size +
      ", scale=" + scale +
      ", isUnique=" + isUnique +
      ", primaryKey=" + primaryKey +
      ", foreignKey=" + foreignKey +
      ", enumType=" + enumType +
      ", valuesOfEnum=" + valuesOfEnum +
      ", foreignEntity='" + foreignTableName + '\'' +
      ", foreignColumnName='" + foreignColumnName + '\'' +
      '}';
  }
}
