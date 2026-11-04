package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.QuestionRequest;
import com.prepmind.prepapi.dto.QuestionResponse;
import com.prepmind.prepapi.entity.Question;
import com.prepmind.prepapi.entity.Sections;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.NoteRepository;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository questionRepository;
    private final ProgressRepository progressRepository;
    private final ProgressEventRepository eventRepository;
    private final NoteRepository noteRepository;

    public QuestionService(QuestionRepository questionRepository,
                           ProgressRepository progressRepository,
                           ProgressEventRepository eventRepository,
                           NoteRepository noteRepository) {
        this.questionRepository = questionRepository;
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.noteRepository = noteRepository;
    }

    /** All filters are optional and case-insensitive. */
    @Transactional(readOnly = true)
    public List<QuestionResponse> list(String section, String company, String difficulty, String category) {
        String sectionF = blankToNull(section) == null ? null : normalizeSection(section);
        String companyF = blankToNull(company);
        String difficultyF = blankToNull(difficulty);
        String categoryF = blankToNull(category);

        return questionRepository.findAllWithCompanies().stream()
                .filter(q -> sectionF == null || sectionF.equals(q.getSection() == null ? Sections.DSA : q.getSection()))
                .filter(q -> difficultyF == null || q.getDifficulty().equalsIgnoreCase(difficultyF))
                .filter(q -> categoryF == null || q.getCategory().equalsIgnoreCase(categoryF))
                .filter(q -> companyF == null || q.getCompanies().stream().anyMatch(c -> c.equalsIgnoreCase(companyF)))
                .map(QuestionResponse::from)
                .toList();
    }

    /** Editors and admins. */
    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Question q = new Question();
        apply(q, request, null);
        log.info("Question created: [{}] {}", q.getSection(), q.getTitle());
        return QuestionResponse.from(questionRepository.save(q));
    }

    /** Editors and admins. Everyone's progress on the question is kept. */
    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question " + id + " does not exist."));
        apply(q, request, id);
        return QuestionResponse.from(questionRepository.save(q));
    }

    /** Editors and admins. Also removes everyone's progress, notes and activity for it. */
    @Transactional
    public void delete(Long id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question " + id + " does not exist."));
        eventRepository.deleteByQuestionId(id);
        progressRepository.deleteByQuestionId(id);
        noteRepository.deleteByQuestionId(id);
        questionRepository.delete(q);
        log.info("Question deleted: [{}] {}", q.getSection(), q.getTitle());
    }

    /** selfId: the question being edited (excluded from duplicate checks), or null when creating. */
    private void apply(Question q, QuestionRequest request, Long selfId) {
        String section = blankToNull(request.section()) == null ? Sections.DSA : normalizeSection(request.section());
        String url = request.problemUrl().trim();
        String title = request.title().trim();
        boolean urlTaken = selfId == null
                ? questionRepository.existsByProblemUrl(url)
                : questionRepository.existsByProblemUrlAndIdNot(url, selfId);
        if (urlTaken) {
            throw ApiException.conflict("QUESTION_EXISTS", "A question with that link already exists.");
        }
        boolean titleTaken = selfId == null
                ? questionRepository.existsBySectionAndTitleIgnoreCase(section, title)
                : questionRepository.existsBySectionAndTitleIgnoreCaseAndIdNot(section, title, selfId);
        if (titleTaken) {
            throw ApiException.conflict("QUESTION_EXISTS", "A question with that title already exists in " + section + ".");
        }
        q.setSection(section);
        q.setTitle(title);
        q.setCategory(request.category().trim());
        q.setDescription(blankToNull(request.description()));
        q.setDifficulty(request.difficulty().toUpperCase(Locale.ROOT));
        q.setProblemUrl(url);
        q.setCompanies(request.companies() == null ? new HashSet<>()
                : request.companies().stream().map(String::trim).filter(c -> !c.isEmpty())
                        .collect(Collectors.toCollection(HashSet::new)));
    }

    private static String normalizeSection(String section) {
        try {
            return Sections.normalize(section);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("UNKNOWN_SECTION", e.getMessage());
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
