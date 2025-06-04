package com.workflow.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the example ULID client service.
 */
@RestController
@RequestMapping("/ulid")
public class ExampleController {

    private final ExampleClientService clientService;

    @Autowired
    public ExampleController(ExampleClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/generate")
    public String generateId() {
        return clientService.generateId();
    }

    @GetMapping("/generate-monotonic")
    public String generateMonotonicId() {
        return clientService.generateMonotonicId();
    }

    @GetMapping("/generate-bulk")
    public List<String> generateBulk(@RequestParam(defaultValue = "10") int count) {
        return clientService.generateBulk(count);
    }

    @GetMapping("/generate-monotonic-bulk")
    public List<String> generateMonotonicBulk(@RequestParam(defaultValue = "10") int count) {
        return clientService.generateMonotonicBulk(count);
    }
}