package com.dico.gen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.dico.gen.model.ChildReference;
import com.dico.gen.model.Entity;
import com.dico.gen.model.Field;

import static com.dico.gen.util.StrUtil.toCamelCase;

public class WriteEntity {

    public static final String IMPORT_PERSISTENCE = "import javax.persistence.";
    public static String FOLDER = "";

    public static void createJavaFile(Entity entity) {

        File file = new File(FOLDER, entity.getEntityTableName().concat(".java"));
        StringBuilder sb = new StringBuilder();
        writeEntity(entity, sb);
        if (entity.hasComposedId()) {
            writeEntityPK(entity);
        }
        if (entity.hasEnum()) {
            writeEnum(entity);
        }

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeEntity(Entity entity, StringBuilder sb) {
        writeImports(entity, sb);
        sb.append("\n@Data");
        sb.append("\n@Entity");
        sb.append("\n@Builder");
        sb.append("\n@NoArgsConstructor");
        sb.append("\n@AllArgsConstructor");
        String uniques = entity.getFields().stream().filter(Field::isUnique)
                .map(field -> "\"" + field.getColumnName() + "\"").collect(Collectors.joining(", "));
        sb.append("\n@Table(name = \"").append(entity.getTableName())
                .append(!uniques.isEmpty()
                        ? "\", uniqueConstraints = {@UniqueConstraint(columnNames = {" + uniques + "})}"
                        : "\"")
                .append(")");
        if (entity.hasEnum()) {
            sb.append("\n@TypeDef(name = \"pgsql_enum\", typeClass = PostgreSQLEnumType.class)");
        }
        sb.append("\npublic class ").append(entity.getEntityTableName()).append(" implements Serializable {");
        sb.append("\n\t/**\r\n"
                + "	 * \r\n"
                + "	 */\r\n"
                + "	private static final long serialVersionUID = 1L;\n\n");
        if (entity.hasComposedId()) {
            generatePropertiesComposedEntity(entity, sb);
        } else {
            generatePropertiesNormalEntity(entity, sb);
        }
    }

    private static void writeEntityPK(Entity entity) {
        String entityNamePK = entity.getEntityTableName() + "PK";
        File file = new File(FOLDER, entityNamePK.concat(".java"));
        StringBuilder sb = new StringBuilder();
        sb.append("\nimport java.io.Serializable;\n")
                .append("\n" + IMPORT_PERSISTENCE + "Column;")
                .append("\n" + IMPORT_PERSISTENCE + "Embeddable;\n");
        writeLombokImports(entity, sb);
        sb.append("\n@Data\r\n"
                + "@Builder\r\n"
                + "@Embeddable\r\n"
                + "@NoArgsConstructor\r\n"
                + "@AllArgsConstructor\r\n")
                .append("public class ").append(entityNamePK).append(" implements Serializable {\r\n")
                .append("\t/**\r\n"
                        + "	 * \r\n"
                        + "	 */\r\n"
                        + "	private static final long serialVersionUID = 1L;\n\n");
        List<Field> composedFields = entity.getComposedFields();
        for (Field field : composedFields) {
            sb.append("\t@Column(name = \"").append(field.getColumnName()).append("\")");
            sb.append("\n\tInteger ").append(field.getFieldName()).append(";\n\n");
        }
        String[] ids = entity.getComposedFields().stream().map(Field::getFieldName).toArray(String[]::new);
        writeEqualsAndHashCode(sb, entityNamePK, ids);
        sb.append("\n}");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeEnum(Entity entity) {
        List<Field> enumFields = entity.getFields().stream().filter(Field::isEnumType).toList();
        for (Field field : enumFields) {
            writeEnumFile(field);
        }

    }

    private static void writeEnumFile(Field field) {
        File enumFolder = new File(FOLDER + "\\enums");
        if (!enumFolder.exists()) {
            enumFolder.mkdirs();
        }
        File file = new File(enumFolder, field.getType().concat(".java"));
        // if (file.exists()) {
        // return;
        // }
        StringBuilder sb = new StringBuilder();
        sb.append("\npublic enum ").append(field.getType()).append(" {\n\t");
        List<String> valuesOfEnum = field.getValuesOfEnum();
        for (int i = 0; i < valuesOfEnum.size(); i++) {
            String s = valuesOfEnum.get(i);
            sb.append(s);
            if (i != valuesOfEnum.size() - 1) {
                sb.append(", ");
            } else {
                sb.append(";");
            }
        }
        sb.append("\n}");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generatePropertiesNormalEntity(Entity entity, StringBuilder sb) {
        for (Field field : entity.getFields()) {
            if (field.isPrimaryKey()) {
                if (field.isForeignKey()) {
                    // sb.append("\n\n\t@GenericGenerator(name = \"generator\", strategy =
                    // \"foreign\", parameters = @Parameter(name = \"property\", value =
                    // \""+formatName(field.getForeignTableName(), true)+"\"))");
                    // sb.append("\n\t@Id");
                    // sb.append("\n\t@GeneratedValue(generator = \"generator\")");
                    // writeColumn(field, sb);
                    //
                    // if (field.getForeignTableName() != null &&
                    // !field.getForeignTableName().isEmpty()) {
                    sb.append("\t@OneToOne(fetch = FetchType.LAZY)");
                    sb.append("\n\t@PrimaryKeyJoinColumn");
                    sb.append("\n\tprivate ").append(toCamelCase(field.getForeignTableName(), true)).append(" ")
                            .append(toCamelCase(field.getForeignTableName(), false)).append(";\n\n");
                    // }
                } else {
                    sb.append("\t@Id");
                    sb.append("\n\t@GeneratedValue(strategy = GenerationType.IDENTITY)");
                    sb.append("\n\t@Column(name = \"").append(field.getColumnName()).append("\")");
                    sb.append("\n\tprivate ").append(field.getType()).append(" ").append(field.getFieldName())
                            .append(";\n\n");
                }
            } else {
                writeColumn(field, sb);
            }
        }

        writeChildrenReferences(entity, sb);
        String[] ids = entity.getFields().stream().filter(Field::isPrimaryKey).map(Field::getFieldName)
                .toArray(String[]::new);
        writeEqualsAndHashCode(sb, entity.getEntityTableName(), ids);
        sb.append("\n}");
    }

    private static void generatePropertiesComposedEntity(Entity entity, StringBuilder sb) {
        generatePropertyEmbeddedId(entity, sb);
        for (Field field : entity.getFields()) {
            if (field.isPrimaryKey() && field.isForeignKey()) {
                sb.append("\t@ManyToOne");
                sb.append("\n\t@JoinColumn(name = \"").append(field.getColumnName()).append("\"");
                sb.append(", insertable = false, updatable = false)");

                String foreignEntityName = toCamelCase(field.getForeignTableName(), true); // ScxCuenta
                String propertyForeignEntity = field.getFieldName(); // idCuentaCierre
                String preffix = field.getForeignTableName().split("_")[0]; // scx
                if (!propertyForeignEntity.contains(preffix)) {
                    propertyForeignEntity = propertyForeignEntity.replaceFirst("id", preffix);
                }
                sb.append("\n\tprivate ").append(foreignEntityName).append(" ").append(propertyForeignEntity)
                        .append(";\n\n");
            } else if (field.isPrimaryKey() && !field.isForeignKey()) {
                sb.append("\t@Column(name = \"")
                        .append(field.getColumnName())
                        .append("\", nullable = false, insertable = false, updatable = false)\r\n")
                        .append("\tprivate ").append(field.getType()).append(" ").append(field.getFieldName())
                        .append(";\n\n");
            } else if (!field.isPrimaryKey()) {
                if (field.getType().equals("LocalDate")) {
                    sb.append("\t@JsonSerialize(using = LocalDateSerializer.class)\r\n"
                            + "	@JsonDeserialize(using = LocalDateDeserializer.class)\r\n");
                }
                if (field.getType().equals("LocalDateTime")) {
                    sb.append("\t@JsonSerialize(using = LocalDateTimeSerializer.class)\r\n"
                            + "	@JsonDeserialize(using = LocalDateTimeDeserializer.class)\r\n");
                }
                if (field.isEnumType()) {
                    sb.append("\t@Enumerated(EnumType.STRING)\n")
                            .append("\t@Column(name = \"").append(field.getColumnName())
                            .append(", columnDefinition = \"").append(field.getColumnDefinition()).append("\")\n")
                            .append("\t@Type(type = \"pgsql_enum\")");
                }
                sb.append("\t@Column(name = \"")
                        .append(field.getColumnName())
                        .append("\"");
                if (field.getType().equals("String")) {
                    sb.append(", length = ")
                            .append(field.getSize());
                }
                // if (field.getIsPrimaryKey() != null && field.getIsPrimaryKey() &&
                // field.getIsForeignKey() != null && field.getIsForeignKey()) {
                // sb.append(", unique = true");
                // }
                // if (field.getIsNull() != null) {
                // sb.append(", nullable = ");
                // sb.append(field.getIsNull());
                // }
                sb.append(")");
                sb.append("\n\tprivate ").append(field.getType()).append(" ").append(field.getFieldName())
                        .append(";\n\n");
            }
        }
        writeEqualsAndHashCode(sb, entity.getEntityTableName(), "id");
        sb.append("\n}");
    }

    private static void generatePropertyEmbeddedId(Entity entity, StringBuilder sb) {
        sb.append("\t@EmbeddedId");
        sb.append("\n\t@AttributeOverrides({");
        List<Field> primaryKeys = entity.getFields().stream().filter(Field::isPrimaryKey).toList();
        for (int i = 0; i < primaryKeys.size(); i++) {
            Field field = primaryKeys.get(i);
            sb.append("\n\t\t@AttributeOverride(name = \"").append(field.getFieldName())
                    .append("\", column = @Column(name = \"").append(field.getColumnName())
                    .append("\", nullable = false))");
            if (i != (primaryKeys.size() - 1)) {
                sb.append(",");
            } else {
                sb.append("})");
            }
        }
        sb.append("\n\tprivate ").append(entity.getEntityTableName()).append("PK id;\n\n");
    }

    private static void writeColumn(Field field, StringBuilder sb) {
        if (field.isForeignKey() && !field.isPrimaryKey()) {
            sb.append("\t@ManyToOne");
            sb.append("\n\t@JoinColumn(name = \"").append(field.getColumnName()).append("\"");
            // if (field.getIsNull() != null) {
            // sb.append(", nullable = ").append(field.getIsNull());
            // }
            sb.append(")");
            String foreignEntityName = toCamelCase(field.getForeignTableName(), true); // ScxCuenta
            String propertyForeignEntity = field.getFieldName(); // idCuentaCierre
            String preffix = field.getForeignTableName().split("_")[0]; // scx
            if (!propertyForeignEntity.contains(preffix)) {
                propertyForeignEntity = propertyForeignEntity.replaceFirst("id", preffix);
            }
            sb.append("\n\tprivate ").append(foreignEntityName).append(" ").append(propertyForeignEntity)
                    .append(";\n\n");
        } else {
            if (field.getType().equals("LocalDate")) {
                sb.append("\t@JsonSerialize(using = LocalDateSerializer.class)\r\n"
                        + "	@JsonDeserialize(using = LocalDateDeserializer.class)\r\n");
            }
            if (field.getType().equals("LocalDateTime")) {
                sb.append("\t@JsonSerialize(using = LocalDateTimeSerializer.class)\r\n"
                        + "	@JsonDeserialize(using = LocalDateTimeDeserializer.class)\r\n");
            }
            if (field.isEnumType()) {
                sb.append("\t@Enumerated(EnumType.STRING)\n");
            }
            sb.append("\t@Column(name = \"")
                    .append(field.getColumnName())
                    .append("\"");
            if (field.getType().equals("String")) {
                sb.append(", length = ")
                        .append(field.getSize());
            }
            if (field.isEnumType()) {
                sb.append(", columnDefinition = \"").append(field.getColumnDefinition()).append("\")\n")
                        .append("\t@Type(type = \"pgsql_enum\"");
            }
            // if (field.getIsPrimaryKey() != null && field.getIsPrimaryKey() &&
            // field.getIsForeignKey() != null && field.getIsForeignKey()) {
            // sb.append(", unique = true");
            // }
            // if (field.getIsNull() != null) {
            // sb.append(", nullable = ");
            // sb.append(field.getIsNull());
            // }
            sb.append(")\n");
            sb.append("\tprivate ").append(field.getType()).append(" ").append(field.getFieldName()).append(";\n\n");
        }
    }

    private static void writeChildrenReferences(Entity entity, StringBuilder sb) {
        if (entity.getChildReferences().isEmpty()) {
            return;
        }
        for (ChildReference childReference : entity.getChildReferences()) {
            sb.append("\t@ToString.Exclude")
                    .append("\n\t@Builder.Default")
                    .append("\n\t@OneToMany(mappedBy = \"");

            String propertyForeignEntity = childReference.getField().getFieldName(); // idCuentaCierre
            String preffix = childReference.getEntity().getTableName().split("_")[0]; // scx
            if (!propertyForeignEntity.contains(preffix)) {
                propertyForeignEntity = propertyForeignEntity.replaceFirst("id", preffix);
            }
            sb.append(propertyForeignEntity);
            String entityTableName = childReference.getEntity().getEntityTableName();
            if (!childReference.getEntity().hasComposedId()) {
                sb.append("\")")
                        .append("\n\tprivate List<").append(entityTableName).append("> ")
                        .append(toPlural(propertyForeignEntity))
                        .append(" = new ArrayList<>();\n\n");
            } else {
                String tableNameProperty = toCamelCase(childReference.getEntity().getTableName(), false);
                String upperCamelCasePropertyForeignEntity = propertyForeignEntity.substring(0, 1).toUpperCase()
                        .concat(propertyForeignEntity.substring(1));
                sb.append("\")")
                        .append("\n\tprivate List<").append(entityTableName).append("> ")
                        .append(toPlural(tableNameProperty))
                        .append("By")
                        .append(upperCamelCasePropertyForeignEntity)
                        .append(" = new ArrayList<>();\n\n");
            }
        }
    }

    public static String toPlural(String word) {
        // Check if the word ends in a vowel (a, e, i, o, u)
        if (word.endsWith("a") || word.endsWith("e") || word.endsWith("o") || word.endsWith("u")) {
            return word + "s";
        }
        // Check if the word ends in "í" or "ú"
        else if (word.endsWith("í") || word.endsWith("ú")) {
            return word + "es";
        }
        // Check if the word ends in "z"
        else if (word.endsWith("z")) {
            return word.substring(0, word.length() - 1) + "ces";
        }
        // Check if the word is a non-acute word ending in "s" or "x"
        else if (isNonAcuteAndEndsInSorX(word)) {
            return word; // Invariable form
        }
        // Check if the word ends in a consonant
        else if (endsInConsonant(word)) {
            return word + "es";
        }
        // Default case (shouldn't be reached)
        return word;
    }

    private static boolean endsInConsonant(String word) {
        char lastChar = word.charAt(word.length() - 1);
        return !isVowel(lastChar) && lastChar != 'í' && lastChar != 'ú' && lastChar != 'z';
    }

    private static boolean isVowel(char c) {
        return "aeiouáéíóú".indexOf(c) != -1;
    }

    private static boolean isNonAcuteAndEndsInSorX(String word) {
        char lastChar = word.charAt(word.length() - 1);
        if (lastChar == 's' || lastChar == 'x') {
            // Check if the word is acute (accented on the last syllable)
            int accentPosition = word.lastIndexOf('á') + word.lastIndexOf('é') + word.lastIndexOf('í')
                    + word.lastIndexOf('ó') + word.lastIndexOf('ú');
            return accentPosition != word.length() - 1; // Not acute
        }
        return false;
    }

    public static void writeImports(Entity entity, StringBuilder sb) {
        writeBasicImports(entity, sb);
        writePersistenceImports(entity, sb);
        if (entity.hasEnum()) {
            writeEnumImports(entity, sb);
        }
        writeJacksonImports(entity, sb);
        writeLombokImports(entity, sb);
    }

    private static void writeEnumImports(Entity entity, StringBuilder sb) {
        sb.append("\nimport org.hibernate.annotations.Type;")
                .append("\nimport org.hibernate.annotations.TypeDef;")
                .append("\nimport com.cladmihalcea.hibernate.type.basic.PostgreSQLEnumType;");
        for (Field field : entity.getFields().stream().filter(Field::isEnumType).toList()) {
            sb.append("\nimport ").append("enums.").append(field.getType()).append(";");
        }

    }

    private static void writePersistenceImports(Entity entity, StringBuilder sb) {
        if (entity.hasComposedId()) {
            sb.append("\n" + IMPORT_PERSISTENCE + "AttributeOverride;\n")
                    .append(IMPORT_PERSISTENCE + "AttributeOverrides;\n")
                    .append(IMPORT_PERSISTENCE + "EmbeddedId;\n")
                    .append(IMPORT_PERSISTENCE + "JoinColumn;\n");
        } else {
            sb.append("\n" + IMPORT_PERSISTENCE + "GeneratedValue;\n");
            if (entity.hasOnePrimaryKey()) {
                sb.append(IMPORT_PERSISTENCE + "GenerationType;\n");
            }
            sb.append(IMPORT_PERSISTENCE + "Id;\n");
        }
        sb.append(IMPORT_PERSISTENCE + "Table;\n")
                .append(IMPORT_PERSISTENCE + "Entity;\n")
                .append(IMPORT_PERSISTENCE + "Column;\n");
        if (entity.hasManyToOne()) {
            sb.append(IMPORT_PERSISTENCE + "ManyToOne;\n");
            sb.append(IMPORT_PERSISTENCE + "JoinColumn;\n");
        }
        if (entity.hasOneToMany()) {
            sb.append(IMPORT_PERSISTENCE + "OneToMany;\n");
        }
        if (entity.hasOneToOneSharedPrimaryKey()) {
            sb.append(IMPORT_PERSISTENCE + "OneToOne;\n");
            sb.append(IMPORT_PERSISTENCE + "PrimaryKeyJoinColumn;\n");
        }
        if (entity.hasUniques()) {
            sb.append(IMPORT_PERSISTENCE + "UniqueConstraint;\n");
        }

        // if (!table.getHasEmbedeed() && table.getFields().stream().anyMatch(field ->
        // field.getIsPrimaryKey() != null && field.getIsForeignKey() != null &&
        // field.getIsPrimaryKey() && field.getIsForeignKey())) {
        // sb.append("\nimport org.hibernate.annotations.GenericGenerator;\r\n"
        // + "import org.hibernate.annotations.Parameter;\n");
        // sb.append(IMPORT_PERSISTENCE + "FetchType;\n");
        // }
    }

    public static void writeBasicImports(Entity entity, StringBuilder sb) {
        if (entity.hasOneToMany()) {
            sb.append("\nimport java.util.ArrayList;\n");
            sb.append("import java.util.List;\n");
        }
        String javaImports = entity.getJavaImports();
        if (!javaImports.isBlank()) {
            sb.append("\n").append(javaImports).append("\n");
        }
        sb.append("import java.io.Serializable;\n");
    }

    private static void writeLombokImports(Entity entity, StringBuilder sb) {
        sb.append("\nimport lombok.Data;\n")
                .append("import lombok.Builder;\n")
                .append("import lombok.AllArgsConstructor;\n")
                .append("import lombok.NoArgsConstructor;\n");
        if (entity.hasOneToMany()) {
            sb.append("import lombok.ToString;\n");
        }
    }

    private static void writeJacksonImports(Entity entity, StringBuilder sb) {
        sb.append("\n");
        if (entity.hasOneToMany()) {
            sb.append("import com.fasterxml.jackson.annotation.JsonIgnore;\n");
        }
        boolean hasLocalDate = entity.anyFieldIsType("LocalDate");
        boolean hasLocalDateTime = entity.anyFieldIsType("LocalDateTime");
        boolean hasDuration = entity.anyFieldIsType("Duration");
        if (hasLocalDate || hasLocalDateTime || hasDuration) {
            sb.append("import com.fasterxml.jackson.databind.annotation.JsonDeserialize;\n"
                    + "import com.fasterxml.jackson.databind.annotation.JsonSerialize;\n");
            if (hasLocalDate) {
                sb.append("import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;\n"
                        + "import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;\n");
            }
            if (hasLocalDateTime) {
                sb.append("import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;\n"
                        + "import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;\n");
            }
            if (hasDuration) {
                sb.append("import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;\n"
                        + "import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;\n");
            }
        }

    }

    private static void writeEqualsAndHashCode(StringBuilder sb, String entityName, String... ids) {
        writeEquals(sb, entityName, ids);
        writeHashCode(sb, ids);
    }

    private static void writeHashCode(StringBuilder sb, String[] ids) {
        sb.append("\t@Override");
        sb.append("\n\tpublic int hashCode() {");
        sb.append("\n\t\tfinal int prime = 31;");
        sb.append("\n\t\tint result = 1;");
        for (String id : ids) {
            sb.append("\n\t\tresult = prime * result + ((").append(id).append(" == null) ? 0 : ").append(id)
                    .append(".hashCode());");
        }
        sb.append("\n\t\treturn result;");
        sb.append("\n\t}");
    }

    private static void writeEquals(StringBuilder sb, String entityName, String[] ids) {
        sb.append("\t@Override");
        sb.append("\n\tpublic boolean equals(Object obj) {");
        sb.append("\n\t\tif (this == obj) {");
        sb.append("\n\t\t\treturn true;");
        sb.append("\n\t\t}");
        sb.append("\n\t\tif (obj == null) {");
        sb.append("\n\t\t\treturn false;");
        sb.append("\n\t\t}");
        sb.append("\n\t\tif (getClass() != obj.getClass()) {");
        sb.append("\n\t\t\treturn false;");
        sb.append("\n\t\t}");
        sb.append("\n\t\t").append(entityName).append(" other = (").append(entityName).append(") obj;");
        for (String id : ids) {
            sb.append("\n\t\tif (").append(id).append(" == null) {");
            sb.append("\n\t\t\tif (other.").append(id).append(" != null) {");
            sb.append("\n\t\t\t\treturn false;");
            sb.append("\n\t\t\t}");
            sb.append("\n\t\t} else if (!").append(id).append(".equals(other.").append(id).append(")) {");
            sb.append("\n\t\t\treturn false;");
            sb.append("\n\t\t}");
            if (ids[ids.length - 1].equals(id)) {
                sb.append("\n\t\treturn true;");
            }
        }
        sb.append("\n\t}\n\n");
    }

}
