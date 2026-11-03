package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.ProgressStatus;
import com.prepmind.prepapi.dto.ProgressView;
import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public ProgressService(ProgressRepository progressRepository,
                           UserRepository userRepository,
                           QuestionRepository questionRepository) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    /** Idempotent: sets the flags to exactly the given values. Both false removes the record. */
    @Transactional
    public ProgressStatus update(Long userId, Long questionId, boolean solved, boolean revised) {
        if (!questionRepository.existsById(questionId)) {
            throw ApiException.notFound("Question " + questionId + " does not exist.");
        }

        Optional<Progress> existing = progressRepository.findByUserIdAndQuestionId(userId, questionId);
        if (!solved && !revised) {
            existing.ifPresent(progressRepository::delete);
            return new ProgressStatus(false, false);
        }

        Progress progress = existing.orElseGet(() -> {
            Progress p = new Progress();
            p.setUser(userRepository.getReferenceById(userId));
            p.setQuestion(questionRepository.getReferenceById(questionId));
            return p;
        });
        progress.setSolved(solved);
        progress.setRevised(revised);
        progressRepository.save(progress);
        return new ProgressStatus(solved, revised);
    }

    /** questionId -> {solved, revised}; questions with no record are simply absent. */
    @Transactional(readOnly = true)
    public Map<Long, ProgressStatus> statusMap(Long userId) {
        Map<Long, ProgressStatus> result = new TreeMap<>();
        for (ProgressView v : progressRepository.findViewsByUserId(userId)) {
            result.put(v.questionId(), new ProgressStatus(v.solved(), v.revised()));
        }
        return result;
    }
}
