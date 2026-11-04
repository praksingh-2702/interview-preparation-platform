package com.prepmind.prepapi.repository;

import com.prepmind.prepapi.dto.NoteResponse;
import com.prepmind.prepapi.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT new com.prepmind.prepapi.dto.NoteResponse(n.question.id, n.content, n.solutionUrl, n.updatedAt) "
            + "FROM Note n WHERE n.user.id = :userId")
    List<NoteResponse> findResponsesByUserId(@Param("userId") Long userId);
}
