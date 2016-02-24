package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.HashMap;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) {
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
                        return new ModelAndView(user, "home.html");
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
                    Game game = new Game(gameName, gameGenre, gamePlatform, releaseYear);
                    user.games.add(game);

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


    }

    static User getUserFromSession(Session session){
        String name = session.attribute("userName");
        return users.get(name);
    }
}
