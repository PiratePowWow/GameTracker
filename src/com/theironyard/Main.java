package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main{

    static HashMap<String, User> users = new HashMap<>();



    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS games (id IDENTITY, userName VARCHAR, name VARCHAR , genre VARCHAR , platform VARCHAR , releaseYear INT)");

        Spark.externalStaticFileLocation("public");

        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    HashMap m = new HashMap();
                    if (user==null){
                        return new ModelAndView(m, "login.html");
                    } else{
                        int editGame = 0;
                        String editGameStr = request.queryParams("editGame");
                        if(editGameStr!=null){
                            editGame = Integer.valueOf(editGameStr);
                            m.put("editGame", editGame);
                        }
                        user.games=selectGames(conn, user);
                        m.put("name", user.name);
                        m.put("games", user.games);


                        return new ModelAndView(m, "home.html");
                    }


                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String name = request.queryParams("loginName");
                    User user = users.get(name);
                    if(name==null){
                        throw new Exception("Login Name Cannot Be Null");
                    }
                    if (user==null){
                        user = new User(name);
                        users.put(name, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", user.name);

                    response.redirect("/");
                    return "";

                })
        );
        Spark.post(
                "/create-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());

                    if(user==null){
                        //throw new Exception("User is not logged in");
                        Spark.halt(403);
                    }


                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int releaseYear = Integer.valueOf(request.queryParams("releaseYear"));
                    if (gameName==null||gameGenre==null||gamePlatform==null){
                        throw new  Exception("Did not receive all queried parameters.");
                    }
                    insertGame(conn,user.name,gameName,gameGenre,gamePlatform,releaseYear);

                    response.redirect("/");


                    return "";
                })
        );
        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/delete-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    String idStr = request.queryParams("id");
                    int id = 0;
                    if(idStr !=null){
                        id = Integer.valueOf(idStr);
                        deleteGame(conn,id);
                    }else{
                        Spark.halt();
                    }
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/edit-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    String idStr = request.queryParams("id");
                    String name = request.queryParams("gameName");
                    String genre = request.queryParams("gameGenre");
                    String platform = request.queryParams("gamePlatform");
                    String releaseYearStr = request.queryParams("releaseYear");
                    int releaseYear = 0;
                    int id = 0;
                    if(idStr !=null){
                        id = Integer.valueOf(idStr);
                        releaseYear = Integer.valueOf(releaseYearStr);
                        editGame(conn,id, name, genre, platform, releaseYear);
                    }else{
                        Spark.halt();
                    }
                    response.redirect("/");
                    return "";
                })
        );


    }

    static ArrayList<Game> selectGames(Connection conn, User user) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("SELECT * FROM games WHERE userName=?");
        stmt2.setString(1, user.name);
        ResultSet results = stmt2.executeQuery();
        ArrayList<Game> games = new ArrayList<>();
        while(results.next()){
            String name = results.getString("name");
            String genre = results.getString("genre");
            String platform = results.getString("platform");
            int releaseYear = results.getInt("releaseYear");
            int id = results.getInt("id");
            Game game = new Game(name, genre, platform, releaseYear, id);
            games.add(game);
        }
        return games;
    }

    static void editGame(Connection conn, int id, String name, String genre, String platform, int releaseYear) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("UPDATE games SET name = ?, genre = ?, platform = ?, releaseYear = ? WHERE id = ?");
        stmt2.setString(1, name);
        stmt2.setString(2, genre);
        stmt2.setString(3, platform);
        stmt2.setInt(4, releaseYear);
        stmt2.setInt(5, id);
        stmt2.execute();
    }

    static void deleteGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM games WHERE id = ?");
        stmt2.setInt(1,id);
        stmt2.execute();

    }

    static void insertGame(Connection conn, String userName, String name, String genre, String platform, int releaseYear) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO games VALUES (NULL,?, ?, ?, ?, ?)");
        stmt2.setString(1, userName);
        stmt2.setString(2, name);
        stmt2.setString(3, genre);
        stmt2.setString(4, platform);
        stmt2.setInt(5, releaseYear);
        stmt2.execute();

    }

    static User getUserFromSession(Session session){
        String name = session.attribute("userName");
        return users.get(name);
    }
}
