package com.qisumei.csgo.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 提取自 Match 的计分板逻辑，负责创建/更新/移除计分板。
 */
public class MatchScoreboardManager implements MatchScoreboard {
    private final Match match;
    private Scoreboard scoreboard;
    private Objective objective;
    private int rebuildCounter = 0;

    public MatchScoreboardManager(Match match) {
        this.match = match;
    }

    public void setupScoreboard() {
        this.scoreboard = match.getServer().getScoreboard();
        String safeMatchName = match.getName().replaceAll("[^a-zA-Z0-9_.-]", "");
        String objectiveName = "kda_" + safeMatchName.substring(0, Math.min(safeMatchName.length(), 12));

        Objective oldObjective = this.scoreboard.getObjective(objectiveName);
        if (oldObjective != null) {
            this.scoreboard.removeObjective(oldObjective);
        }

        this.objective = this.scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            Component.literal("比赛排名").withStyle(ChatFormatting.YELLOW),
            ObjectiveCriteria.RenderType.INTEGER,
            true,
            null
        );
        this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
    }

    public void updateScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;

        rebuildCounter++;
        if (rebuildCounter >= 200) {
            rebuildScoreboard();
            rebuildCounter = 0;
            return;
        }

        List<Map.Entry<UUID, PlayerStats>> sortedPlayers = match.getPlayerStats().entrySet().stream()
            .sorted((a, b) -> {
                int cmp = Integer.compare(b.getValue().getKills(), a.getValue().getKills());
                if (cmp != 0) return cmp;
                return Integer.compare(a.getValue().getDeaths(), b.getValue().getDeaths());
            })
            .limit(15)
            .toList();

        for (Map.Entry<UUID, PlayerStats> entry : sortedPlayers) {
            ServerPlayer player = match.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                this.scoreboard.getOrCreatePlayerScore(player, this.objective).set(entry.getValue().getKills());
            }
        }
    }

    private void rebuildScoreboard() {
        if (this.objective == null || this.scoreboard == null) return;

        String objectiveName = this.objective.getName();
        Component displayName = this.objective.getDisplayName();

        this.scoreboard.removeObjective(this.objective);

        this.objective = this.scoreboard.addObjective(
            objectiveName,
            ObjectiveCriteria.DUMMY,
            displayName,
            ObjectiveCriteria.RenderType.INTEGER,
            true,
            null
        );
        this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
    }

    public void reapplyToPlayer(ServerPlayer player) {
        if (this.objective == null || this.scoreboard == null) return;
        this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, this.objective);
        PlayerStats stats = match.getPlayerStats().get(player.getUUID());
        int currentKills = (stats != null) ? stats.getKills() : 0;
        this.scoreboard.getOrCreatePlayerScore(player, this.objective).set(currentKills);
    }

    public void removeScoreboard() {
        if (this.scoreboard != null && this.objective != null) {
            this.scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
            Objective currentObjective = this.scoreboard.getObjective(this.objective.getName());
            if (currentObjective != null) {
                this.scoreboard.removeObjective(currentObjective);
            }
            this.objective = null;
        }
    }
}
