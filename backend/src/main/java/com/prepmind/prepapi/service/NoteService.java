package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.NoteRequest;
import com.prepmind.prepapi.dto.NoteResponse;
import com.prepmind.prepapi.entity.Note;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.NoteRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository,
                       QuestionRepository questionRepository, Clock clock) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<Long, NoteResponse> list(Long userId) {
        Map<Long, NoteResponse> result = new TreeMap<>();
        noteRepository.findResponsesByUserId(userId).forEach(n -> result.put(n.questionId(), n));
        return result;
    }

    /** Creates or replaces the note; returns empty when the request clears it (note deleted). */
    @Transactional
    public Optional<NoteResponse> save(Long userId, Long questionId, NoteRequest request) {
        if (!questionRepository.existsById(questionId)) {
            throw ApiException.notFound("Question " + questionId + " does not exist.");
        }
        String content = trimToNull(request.content());
        String url = trimToNull(request.solutionUrl());
        Optional<Note> existing = noteRepository.findByUserIdAndQuestionId(userId, questionId);

        if (content == null && url == null) {
            existing.ifPresent(noteRepository::delete);
            return Optional.empty();
        }

        Note note = existing.orElseGet(() -> {
            Note n = new Note();
            n.setUser(userRepository.getReferenceById(userId));
            n.setQuestion(questionRepository.getReferenceById(questionId));
            return n;
        });
        note.setContent(content);
        note.setSolutionUrl(url);
        note.setUpdatedAt(clock.instant());
        noteRepository.save(note);
        return Optional.of(new NoteResponse(questionId, content, url, note.getUpdatedAt()));
    }

    @Transactional
    public void delete(Long userId, Long questionId) {
        noteRepository.findByUserIdAndQuestionId(userId, questionId).ifPresent(noteRepository::delete);
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
