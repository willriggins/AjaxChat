package com.theironyard;

import jodd.json.JsonParser;
import jodd.json.JsonSerializer;
import org.h2.tools.Server;
import spark.Spark;

import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (id IDENTITY, author VARCHAR, text VARCHAR)");
    }

    public static void insertMessage(Connection conn, Message msg) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?)");
        stmt.setString(1, msg.author);
        stmt.setString(2, msg.text);
        stmt.execute();
    }

    public static ArrayList<Message> selectMessages(Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM messages");
        ResultSet results = stmt.executeQuery();
        ArrayList<Message> msgs = new ArrayList<>();
        while (results.next()) {
            int id = results.getInt("id");
            String author = results.getString("author");
            String text = results.getString("text");
            Message msg = new Message(id, author, text);
            msgs.add(msg);
        }
        return msgs;
    }

    public static void main(String[] args) throws SQLException {

        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        Spark.externalStaticFileLocation("public");
        Spark.init();

        Spark.get(
                "/get-messages",
                (request, response) -> {
                    ArrayList<Message> msgs = selectMessages(conn);
                    JsonSerializer s = new JsonSerializer();
                    return s.serialize(msgs);
                }
        );
        Spark.post(
                "/add-message",
                (request, response) -> {
                    String body = request.body(); // this string will contain the json.. now parse it into a msg object right away
                    JsonParser p = new JsonParser();
                    Message msg = p.parse(body, Message.class);
                    insertMessage(conn, msg);
                    return "";
                }
        );
    }
}
