package com.example.demo.constants;

import java.util.List;

public class Routes {

    public static final List<String> open_routes = List.of(
            "/api/auth/login",
            "/h2-console/**");

    public static final List<String> societe_routes = List.of(
            "/api/societe");

    public static final List<String> comptable_routes = List.of(
            "/api/comptable");
}
