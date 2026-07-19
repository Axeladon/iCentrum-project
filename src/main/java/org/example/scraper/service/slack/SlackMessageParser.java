package org.example.scraper.service.slack;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlackMessageParser {

    private static final Pattern GROUP_NUMBER_PATTERN =
            Pattern.compile("^\\s*([1-9]\\d{0,2})\\.");

    public OptionalInt extractGroupNumber(String message) {
        if (message == null || message.isBlank()) {
            return OptionalInt.empty();
        }

        Matcher matcher = GROUP_NUMBER_PATTERN.matcher(message);

        if (matcher.lookingAt()) {
            return OptionalInt.of(Integer.parseInt(matcher.group(1)));
        }

        return OptionalInt.empty();
    }
}