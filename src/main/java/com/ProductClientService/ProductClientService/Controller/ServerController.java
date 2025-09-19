package com.ProductClientService.ProductClientService.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {

    @GetMapping()
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
