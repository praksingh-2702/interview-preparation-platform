package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProgressService {

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    public Progress markQuestionProgress(String username, Long questionId, String status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

        Optional<Progress> existingProgress = progressRepository.findByUserAndQuestion(user, question);

        Progress progress;
        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            progress.setStatus(status); 
        } else {
            progress = new Progress();
            progress.setUser(user);
            progress.setQuestion(question);
            progress.setStatus(status);
        }

        return progressRepository.save(progress);
    }

    public List<Progress> getUserProgress(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Updated to perfectly match your repository method
        return progressRepository.findByUser_Id(user.getId()); 
    }
}