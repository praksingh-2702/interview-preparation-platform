package com.prepmind.prepapi.controller;

import com.prepmind.prepapi.dto.NoteRequest;
import com.prepmind.prepapi.dto.NoteResponse;
import com.prepmind.prepapi.security.AuthenticatedUser;
import com.prepmind.prepapi.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /** { "<questionId>": { questionId, content, solutionUrl, updatedAt }, ... } */
    @GetMapping
    public Map<Long, NoteResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return noteService.list(user.id());
    }

    /** 200 with the saved note, or 204 if both fields were blank (note removed). */
    @PutMapping("/{questionId}")
    public ResponseEntity<NoteResponse> save(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable Long questionId,
                                             @Valid @RequestBody NoteRequest request) {
        return noteService.save(user.id(), questionId, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long questionId) {
        noteService.delete(user.id(), questionId);
        return ResponseEntity.noContent().build();
    }
}
