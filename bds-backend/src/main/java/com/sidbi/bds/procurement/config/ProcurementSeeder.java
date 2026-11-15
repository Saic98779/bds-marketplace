package com.sidbi.bds.procurement.config;

import com.sidbi.bds.procurement.domain.FeedbackQuestion;
import com.sidbi.bds.procurement.domain.ProcEnums;
import com.sidbi.bds.procurement.repository.FeedbackQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcurementSeeder implements ApplicationRunner {

    private final FeedbackQuestionRepository questionRepository;

    private record Seed(String text, ProcEnums.QuestionType type, boolean mandatory) {
    }

    private static final List<Seed> QUESTIONS = List.of(
            new Seed("Were the deliverables provided as per the agreed scope?", ProcEnums.QuestionType.YES_NO, true),
            new Seed("Quality of deliverables", ProcEnums.QuestionType.RATING, true),
            new Seed("Timeliness of delivery against the agreed schedule", ProcEnums.QuestionType.RATING, true),
            new Seed("Communication and responsiveness of the provider", ProcEnums.QuestionType.RATING, true),
            new Seed("Value for money", ProcEnums.QuestionType.RATING, false),
            new Seed("Would you engage this provider again?", ProcEnums.QuestionType.YES_NO, true),
            new Seed("What could the provider have done better?", ProcEnums.QuestionType.TEXT, false)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() > 0) {
            return;
        }
        int order = 1;
        for (Seed seed : QUESTIONS) {
            FeedbackQuestion question = new FeedbackQuestion();
            question.setQuestionText(seed.text());
            question.setQuestionType(seed.type());
            question.setMandatory(seed.mandatory());
            question.setDisplayOrder(order++);
            question.setActive(true);
            questionRepository.save(question);
        }
    }
}
