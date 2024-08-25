package com.dico.gen;

import com.dico.gen.db.DBUtil;
import com.dico.gen.model.ChildReference;
import com.dico.gen.model.Entity;
import com.dico.gen.model.Field;

import javax.swing.*;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseConfigWindow window = new DatabaseConfigWindow();
            window.setVisible(true);
        });

    }

}
