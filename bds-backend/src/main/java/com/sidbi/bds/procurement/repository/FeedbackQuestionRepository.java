package com.sidbi.bds.procurement.repository;

import com.sidbi.bds.procurement.domain.FeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {

    List<FeedbackQuestion> findByActiveTrueOrderByDisplayOrderAsc();

    List<FeedbackQuestion> findAllByOrderByDisplayOrderAsc();
}
