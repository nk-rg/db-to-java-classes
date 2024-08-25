package com.dico.gen.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBUtil {
  private final String url;
  private final String username;
  private final String password;

  public DBUtil(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }


  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }

}
