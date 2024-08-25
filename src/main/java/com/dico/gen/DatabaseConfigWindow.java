package com.dico.gen;

import com.dico.gen.db.DBUtil;
import com.dico.gen.model.ChildReference;
import com.dico.gen.model.Entity;
import com.dico.gen.model.Field;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import static com.dico.gen.util.StrUtil.toCamelCase;

public class DatabaseConfigWindow extends JFrame {

    private final JTextField jdbcField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private JTextField catalogField;
    private final JTextField schemaField;
    private final JTextField directoryField;

    private String schemaName;
    private String catalog;

    public DatabaseConfigWindow() {
        setTitle("Database Configuration");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        // JDBC URL
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("JDBC URL:"), gbc);

        jdbcField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(jdbcField, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);

        usernameField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField();
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(passwordField, gbc);

        // Schema
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Schema:"), gbc);

        schemaField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(schemaField, gbc);

        // Catalog
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("Catalog:"), gbc);

        catalogField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(catalogField, gbc);

        // Directory
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("Directory:"), gbc);

        directoryField = new JTextField();
        directoryField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 5;
        add(directoryField, gbc);

        // Browse Button
        JButton browseButton = new JButton("Browse...");
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(browseButton, gbc);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseDirectory();
            }
        });

        // Submit button
        JButton submitButton = new JButton("Submit");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        add(submitButton, gbc);

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitForm();
            }
        });
    }

    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            directoryField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void submitForm() {
        String jdbcUrl = jdbcField.getText();
        String username = usernameField.getText();
        String password = String.valueOf(passwordField.getPassword());
        this.catalog = catalogField.getText();
        this.schemaName = schemaField.getText();
        String directory = directoryField.getText();
        WriteEntity.FOLDER = directory;

        initLoadMetadata(jdbcUrl, username, password);
    }

    private void initLoadMetadata(String jdbcUrl, String username, String password) {
        DBUtil dbUtil = new DBUtil(jdbcUrl, username, password);
        try (Connection connection = dbUtil.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(this.catalog, this.schemaName, "%", new String[] { "TABLE" });

            List<Entity> entities = createEntities(tables, metaData, connection);

            entities.forEach(entity -> entity.getFields().stream()
                    .filter(Field::isForeignKey)
                    .forEach(field -> {
                        String foreignTableName = field.getForeignTableName();
                        String formattedForeignTableName = toCamelCase(foreignTableName, true);
                        Optional<Entity> first = entities.stream()
                                .filter(enti -> enti.getEntityTableName().equals(formattedForeignTableName))
                                .findFirst();
                        first.ifPresent(value -> value.getChildReferences().add(new ChildReference(entity, field)));
                    }));
            createJavaFiles(entities);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createJavaFiles(List<Entity> entities) {
        for (Entity entity : entities) {
            WriteEntity.createJavaFile(entity);
        }
    }

    private List<Entity> createEntities(ResultSet tables, DatabaseMetaData metaData, Connection connection)
            throws SQLException {
        List<Entity> entities = new ArrayList<>();
        while (tables.next()) {
            Entity entity = new Entity();
            String tableName = tables.getString("TABLE_NAME");
            entity.setEntityTableName(toCamelCase(tableName, true));
            entity.setTableName(tableName);
            List<Field> fields = getFieldsFromTableName(metaData, tableName, connection);
            entity.setFields(fields);
            entities.add(entity);
        }
        return entities;
    }

    private List<Field> getFieldsFromTableName(DatabaseMetaData metaData, String tableName, Connection connection)
            throws SQLException {
        ResultSet metaDataColumns = metaData.getColumns(this.catalog, this.schemaName, tableName, "%");
        ResultSet primaryKeys = metaData.getPrimaryKeys(this.catalog, this.schemaName, tableName);
        ResultSet foreignKeys = metaData.getImportedKeys(this.catalog, this.schemaName, tableName);
        ResultSet uniqueIndexes = metaData.getIndexInfo(this.catalog, this.schemaName, tableName, true, true);
        // metaData.getTypeInfo()
        Set<String> uniqueColumns = new HashSet<>();
        while (uniqueIndexes.next()) {
            String indexName = uniqueIndexes.getString("INDEX_NAME");
            String columnName = uniqueIndexes.getString("COLUMN_NAME");
            if (columnName != null) {
                uniqueColumns.add(columnName);
            }
        }

        List<Field> fields = new ArrayList<>();
        while (metaDataColumns.next()) {

            Field field = new Field();

            String columnName = metaDataColumns.getString("COLUMN_NAME");
            String typeName = metaDataColumns.getString("TYPE_NAME");
            int columnSize = metaDataColumns.getInt("COLUMN_SIZE");
            int decimalDigits = metaDataColumns.getInt("DECIMAL_DIGITS");

            field.setColumnName(columnName);
            field.setFieldName(toCamelCase(columnName, false));
            field.setSize(columnSize);
            field.setScale(decimalDigits);
            field.setType(getJavaType(typeName));

            if (isEnumColumn(connection, tableName, columnName)) {
                String enumColumnDefinition = getEnumColumnDefinition(connection, tableName, columnName);
                List<String> enumValues = getEnumValues(connection, tableName, columnName);
                field.setEnumType(true);
                field.setValuesOfEnum(enumValues);
                field.setColumnDefinition(enumColumnDefinition);
                field.setType("Enum" + toCamelCase(field.getColumnName(), true));
            }
            if (uniqueColumns.contains(columnName)) {
                field.setUnique(true);
            }
            fields.add(field);
        }

        while (primaryKeys.next()) {
            String primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
            fields.stream().filter(field -> field.getFieldName().equals(toCamelCase(primaryKeyColumn, false)))
                    .findFirst()
                    .ifPresent(field -> {
                        field.setPrimaryKey(true);
                        field.setUnique(false);
                    });
        }

        while (foreignKeys.next()) {
            String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
            String pkTableName = foreignKeys.getString("PKTABLE_NAME");
            String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
            Optional<Field> first = fields.stream()
                    .filter(field -> field.getFieldName().equals(toCamelCase(fkColumnName, false)))
                    .findFirst();
            first.ifPresent(field -> {
                field.setForeignKey(true);
                field.setForeignTableName(pkTableName);
                field.setForeignColumnName(pkColumnName);
                if (!field.isPrimaryKey() && uniqueColumns.contains(fkColumnName)) {
                    field.setUnique(true);
                }
            });

        }

        return fields;
    }

    private static boolean isEnumColumn(Connection connection, String tableName, String columnName)
            throws SQLException {
        String query = "SELECT t.typname AS enum_type " +
                "FROM pg_type t " +
                "JOIN pg_enum e ON t.oid = e.enumtypid " +
                "JOIN pg_attribute a ON a.atttypid = t.oid " +
                "JOIN pg_class c ON c.oid = a.attrelid " +
                "WHERE c.relname = ? AND a.attname = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String getEnumColumnDefinition(Connection connection, String tableName, String columnName)
            throws SQLException {
        String query = "SELECT pg_catalog.format_type(a.atttypid, a.atttypmod) AS column_definition " +
                "FROM pg_attribute a " +
                "JOIN pg_class c ON c.oid = a.attrelid " +
                "WHERE c.relname = ? AND a.attname = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("column_definition");
                } else {
                    return null; // or throw an exception if preferred
                }
            }
        }
    }

    private static List<String> getEnumValues(Connection connection, String tableName, String columnName)
            throws SQLException {
        String query = "SELECT e.enumlabel AS enum_value " +
                "FROM pg_type t " +
                "JOIN pg_enum e ON t.oid = e.enumtypid " +
                "JOIN pg_attribute a ON a.atttypid = t.oid " +
                "JOIN pg_class c ON c.oid = a.attrelid " +
                "WHERE c.relname = ? AND a.attname = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> enumValues = new ArrayList<>();
                while (rs.next()) {
                    enumValues.add(rs.getString("enum_value"));
                }
                return enumValues;
            }
        }
    }

    // private static boolean isEnumColumn(Connection connection, String tableName,
    // String columnName) throws SQLException {
    // String query = "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE
    // TABLE_NAME = ? AND COLUMN_NAME = ?";
    // try (PreparedStatement stmt = connection.prepareStatement(query)) {
    // stmt.setString(1, tableName);
    // stmt.setString(2, columnName);
    // try (ResultSet rs = stmt.executeQuery()) {
    // if (rs.next()) {
    // String dataType = rs.getString("DATA_TYPE");
    // return "enum".equalsIgnoreCase(dataType) ||
    // "USER-DEFINED".equalsIgnoreCase(dataType);
    // }
    // }
    // }
    // return false;
    // }

    // private static List<String> getEnumValues(Connection connection, String
    // tableName, String columnName) throws SQLException {
    // String query = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE
    // TABLE_NAME = ? AND COLUMN_NAME = ?";
    // try (PreparedStatement stmt = connection.prepareStatement(query)) {
    // stmt.setString(1, tableName);
    // stmt.setString(2, columnName);
    // try (ResultSet rs = stmt.executeQuery()) {
    // if (rs.next()) {
    // String columnType = rs.getString("COLUMN_TYPE");
    // return Arrays.asList(columnType.substring(columnType.indexOf("(") + 1,
    // columnType.indexOf(")")).replace("'", "").split(","));
    // }
    // }
    // }
    // return Collections.emptyList();
    // }

    private static String getJavaType(String typeName) {
        return switch (typeName) {
            case "varchar" -> "String";
            case "bool" -> "Boolean";
            case "int4", "serial" -> "Integer";
            case "date" -> "LocalDate";
            case "timestamp" -> "LocalDateTime";
            case "numeric" -> "BigDecimal";
            default -> typeName;
        };
    }

}
