package dev.sebi.sebchievements.core;

import dev.sebi.sebchievements.handlers.silly.TestHandler;

import java.util.List;

/** The one explicit list of handlers. Adding a handler = one class + one line here. */
public final class Handlers {
    public static final List<AdvancementHandler> ALL = List.of(
            new TestHandler()
    );

    private Handlers() {}
}
