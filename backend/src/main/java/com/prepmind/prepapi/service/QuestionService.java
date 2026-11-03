package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.QuestionRequest;
import com.prepmind.prepapi.dto.QuestionResponse;
import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /** All filters are optional and case-insensitive. */
    @Transactional(readOnly = true)
    public List<QuestionResponse> list(String company, String difficulty, String category) {
        String companyF = blankToNull(company);
        String difficultyF = blankToNull(difficulty);
        String categoryF = blankToNull(category);

        return questionRepository.findAllWithCompanies().stream()
                .filter(q -> difficultyF == null || q.getDifficulty().equalsIgnoreCase(difficultyF))
                .filter(q -> categoryF == null || q.getCategory().equalsIgnoreCase(categoryF))
                .filter(q -> companyF == null || q.getCompanies().stream().anyMatch(c -> c.equalsIgnoreCase(companyF)))
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        String url = request.problemUrl().trim();
        if (questionRepository.existsByProblemUrl(url)) {
            throw ApiException.conflict("QUESTION_EXISTS", "A question with that problem URL already exists.");
        }
        Question q = new Question();
        q.setTitle(request.title().trim());
        q.setCategory(request.category().trim());
        q.setDescription(request.description() == null ? null : request.description().trim());
        q.setDifficulty(request.difficulty().toUpperCase(Locale.ROOT));
        q.setProblemUrl(url);
        q.setCompanies(request.companies() == null ? new HashSet<>()
                : request.companies().stream().map(String::trim).collect(Collectors.toCollection(HashSet::new)));
        return QuestionResponse.from(questionRepository.save(q));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
