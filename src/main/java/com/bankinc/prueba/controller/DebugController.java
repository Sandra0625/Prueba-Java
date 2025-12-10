package com.bankinc.prueba.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class DebugController {

    @GetMapping("/api/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> ui() throws IOException {
        Resource res = new ClassPathResource("static/index.html");
        if (!res.exists()) return ResponseEntity.notFound().build();
        String html = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(html);
    }
}
