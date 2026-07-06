package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000") // 👈 This unlocks the connection for your dashboard!
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionRepository questionRepository;

    // GET request to fetch all questions, with an optional company filter
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions(@RequestParam(required = false) String company) {
        List<Question> questions;
        
        if (company != null && !company.trim().isEmpty()) {
            questions = questionRepository.findByCompanyName(company);
        } else {
            questions = questionRepository.findAll();
        }
        
        return ResponseEntity.ok(questions);
    }

    // POST request to add a new question (for testing)
    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question savedQuestion = questionRepository.save(question);
        return ResponseEntity.ok(savedQuestion);
    }
}