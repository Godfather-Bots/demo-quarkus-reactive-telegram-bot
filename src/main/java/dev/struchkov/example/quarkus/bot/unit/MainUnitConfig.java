package dev.struchkov.example.quarkus.bot.unit;

import dev.struchkov.example.quarkus.bot.util.UnitName;
import dev.struchkov.godfather.main.domain.BoxAnswer;
import dev.struchkov.godfather.main.domain.annotation.Unit;
import dev.struchkov.godfather.main.domain.content.Mail;
import dev.struchkov.godfather.quarkus.core.unit.AnswerText;
import dev.struchkov.godfather.quarkus.core.unit.MainUnit;

import javax.inject.Singleton;

import static dev.struchkov.example.quarkus.bot.util.UnitName.NAME_UNIT_TWO;

@Singleton
public class MainUnitConfig {

    @Unit(value = UnitName.NAME_UNIT_ONE, main = true)
    public AnswerText<Mail> unitOne(
            @Unit(NAME_UNIT_TWO) MainUnit<Mail> unitTwo
    ) {
        return AnswerText.<Mail>builder()
                .answer(BoxAnswer.boxAnswer("Hello"))
                .next(unitTwo)
                .build();
    }

    @Unit(value = NAME_UNIT_TWO)
    public AnswerText<Mail> unitTwo() {
        return AnswerText.<Mail>builder()
                .triggerPhrase("hi", "hello", "привет")
                .answer(BoxAnswer.boxAnswer("How are you?"))
                .build();
    }

}
