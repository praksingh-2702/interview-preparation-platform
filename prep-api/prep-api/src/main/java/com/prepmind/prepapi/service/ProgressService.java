package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.repository.UserRepository;
import com.prepmind.prepapi.repository.UserProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🚀 IMPORT THIS

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
public class ProgressService {

    @Autowired
    private UserProgressRepository userProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Transactional // 🚀 ADD THIS TO ENABLE SAFE DB WRITING/DELETING
    public void updateProgress(String username, Long questionId, boolean isSolved, boolean isRevised) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        
        if (!userOpt.isPresent() || !questionOpt.isPresent()) {
            throw new RuntimeException("Error: Invalid user or question identity context.");
        }
        
        User user = userOpt.get();
        Question question = questionOpt.get();
        
        Optional<Progress> existingProgress = userProgressRepository.findByUserIdAndQuestionId(user.getId(), question.getId());

        if (!isSolved && !isRevised) {
            existingProgress.ifPresent(progress -> userProgressRepository.delete(progress));
            return;
        }

        Progress progress = existingProgress.orElse(new Progress());
        if (progress.getId() == null) {
            progress.setUser(user);
            progress.setQuestion(question);
        }

        progress.setSolved(isSolved);
        progress.setRevised(isRevised);

        userProgressRepository.save(progress);
    }

    public Map<Long, Map<String, Boolean>> getUserProgressMap(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
                
        List<Progress> progressList = userProgressRepository.findByUserId(user.getId());
        Map<Long, Map<String, Boolean>> resultMap = new HashMap<>();
        
        for (Progress p : progressList) {
            Map<String, Boolean> states = new HashMap<>();
            states.put("solved", p.isSolved());
            states.put("revised", p.isRevised());
            resultMap.put(p.getQuestion().getId(), states);
        }
        
        return resultMap;
    }
}