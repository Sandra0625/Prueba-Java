package com.bankinc.prueba.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class HomeController {

    // Serve index.html directly from classpath for both '/' and '/index.html'
    @GetMapping({"/", "/index.html"})
    public ResponseEntity<Resource> home() throws IOException {
        Resource resource = new ClassPathResource("static/index.html");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(resource);
    }
}
