package com.example.student;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Привет! Это мой первый эндпоинт.";
    }

    @GetMapping("/number")
    public int getNumber() {
        return 13;
    }
}