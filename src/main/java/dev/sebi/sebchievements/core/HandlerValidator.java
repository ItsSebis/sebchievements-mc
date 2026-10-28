package dev.sebi.sebchievements.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup checks. Problems are logged loudly and only the offending handler is dropped,
 * never the whole plugin.
 */
public final class HandlerValidator {
    private HandlerValidator() {}

    /**
     * @param pluginIds  ids with {@code mode: plugin}
     * @param allIds     every known id
     * @param trackedIds timed/streak ids
     * @param loaded     whether the server actually loaded the advancement
     * @return the handlers that passed
     */
    public static List<AdvancementHandler> validate(Collection<AdvancementHandler> handlers, Set<String> pluginIds,
                                                    Set<String> allIds, Set<String> trackedIds,
                                                    Predicate<String> loaded, Logger log) {
        Map<String, List<AdvancementHandler>> byId = new HashMap<>();
        for (AdvancementHandler h : handlers) byId.computeIfAbsent(h.id(), x -> new ArrayList<>()).add(h);

        List<AdvancementHandler> ok = new ArrayList<>();
        for (AdvancementHandler h : handlers) {
            String id = h.id();
            String problem = null;
            if (!allIds.contains(id)) problem = "id is not in the catalog";
            else if (!pluginIds.contains(id)) problem = "catalog entry is not mode: plugin";
            else if (byId.get(id).size() > 1) problem = "more than one handler registered for this id";
            else if (!loaded.test(id)) problem = "advancement is not loaded on the server (datapack missing?)";
            else if (trackedIds.contains(id) && !(h instanceof ProgressReporting)) {
                problem = "timed/streak advancement but the handler does not implement ProgressReporting";
            }
            if (problem == null) ok.add(h);
            else log.log(Level.SEVERE, "Handler " + h.getClass().getSimpleName() + " (" + id + ") disabled: " + problem);
        }
        for (String id : pluginIds) {
            if (!byId.containsKey(id)) log.log(Level.SEVERE, "Plugin advancement '" + id + "' has no handler; it can never be earned");
        }
        return ok;
    }
}
