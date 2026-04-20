package com.example.papergenerator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    /** Open endpoint — confirms the backend is reachable without auth. */
    @GetMapping
    public String ping() {
        return "OK";
    }
}
