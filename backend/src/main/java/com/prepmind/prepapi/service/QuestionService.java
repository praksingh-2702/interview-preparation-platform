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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    /**
     * Every question, ready to serve. The bank only changes when an editor edits it (or on deploy), so
     * dashboard loads skip the database. Dropped after any change commits; rebuilt on the next read.
     */
    private volatile List<QuestionResponse> cache;

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

        List<QuestionResponse> all = all();
        if (sectionF == null && companyF == null && difficultyF == null && categoryF == null) {
            return all;
        }
        return all.stream()
                .filter(q -> sectionF == null || sectionF.equals(q.section()))
                .filter(q -> difficultyF == null || q.difficulty().equalsIgnoreCase(difficultyF))
                .filter(q -> categoryF == null || q.category().equalsIgnoreCase(categoryF))
                .filter(q -> companyF == null || q.companies().stream().anyMatch(c -> c.equalsIgnoreCase(companyF)))
                .toList();
    }

    private List<QuestionResponse> all() {
        List<QuestionResponse> current = cache;
        if (current == null) {
            current = questionRepository.findAllWithCompanies().stream().map(QuestionResponse::from).toList();
            cache = current;
        }
        return current;
    }

    /** Drops the cached list once the current transaction commits (or right away outside one). */
    public void invalidateCache() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache = null;
                }
            });
        } else {
            cache = null;
        }
    }

    /** Editors and admins. */
    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        Question q = new Question();
        apply(q, request, null);
        log.info("Question created: [{}] {}", q.getSection(), q.getTitle());
        QuestionResponse saved = QuestionResponse.from(questionRepository.save(q));
        invalidateCache();
        return saved;
    }

    /** Editors and admins. Everyone's progress on the question is kept. */
    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Question " + id + " does not exist."));
        apply(q, request, id);
        QuestionResponse saved = QuestionResponse.from(questionRepository.save(q));
        invalidateCache();
        return saved;
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
        invalidateCache();
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
