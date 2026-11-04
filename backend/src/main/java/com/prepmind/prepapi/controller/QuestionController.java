package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.QuestionRequest;
import com.prepmind.prepapi.dto.QuestionResponse;
import com.prepmind.prepapi.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<QuestionResponse> list(@RequestParam(required = false) String section,
                                       @RequestParam(required = false) String company,
                                       @RequestParam(required = false) String difficulty,
                                       @RequestParam(required = false) String category) {
        return questionService.list(section, company, difficulty, category);
    }

    // Writes: EDITOR or ADMIN (see WebSecurityConfig).

    @PostMapping
    public ResponseEntity<QuestionResponse> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
