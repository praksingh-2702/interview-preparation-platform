package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    // Fixed: Changed from findAllDistinct() to standard findAll()
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    // Works perfectly with your custom @Query method now
    public List<Question> getQuestionsByCompany(String company) {
        return questionRepository.findByCompanyName(company);
    }

    public List<Question> getQuestionsByDifficulty(String difficulty) {
        return questionRepository.findByDifficulty(difficulty);
    }

    // Connects the POST controller endpoint to the repository layer
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }
}