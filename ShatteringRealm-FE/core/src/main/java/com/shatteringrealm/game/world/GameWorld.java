package com.shatteringrealm.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;

// World stage

public class GameWorld {

    public static final float PLATFORM_HEIGHT = 12f;

    private final float worldWidth;
    private final float groundY;
    private final List<Platform> platforms;

    public GameWorld() {
        worldWidth = 1200f;
        groundY = 0f;

        platforms = new ArrayList<>();

        // ── Ground ──────────────────────────────────────────
        platforms.add(new Platform(0, groundY, worldWidth, PLATFORM_HEIGHT));

        // ── Platforms for testing ───────────────────────────
        // Stepping stones (varying heights to test jump arcs)
        platforms.add(new Platform(120, 50, 80, PLATFORM_HEIGHT));
        platforms.add(new Platform(240, 90, 80, PLATFORM_HEIGHT));
        platforms.add(new Platform(360, 130, 80, PLATFORM_HEIGHT));

        // Wall jump corridor — two tall walls facing each other
        platforms.add(new Platform(500, PLATFORM_HEIGHT, 15, 160));   // left wall
        platforms.add(new Platform(585, PLATFORM_HEIGHT, 15, 160));   // right wall
        platforms.add(new Platform(500, 170, 100, PLATFORM_HEIGHT));  // top platform

        // Long high platform with a gap
        platforms.add(new Platform(650, 60, 120, PLATFORM_HEIGHT));
        // gap here — test fall
        platforms.add(new Platform(820, 60, 120, PLATFORM_HEIGHT));

        // Higher platform above the gap
        platforms.add(new Platform(730, 130, 100, PLATFORM_HEIGHT));

        // Extended area
        platforms.add(new Platform(960, 40, 100, PLATFORM_HEIGHT));
        platforms.add(new Platform(1050, 90, 80, PLATFORM_HEIGHT));
        platforms.add(new Platform(1080, 160, 100, PLATFORM_HEIGHT));
    }

    public void render(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (Platform p : platforms) {
            if (p == platforms.get(0)) {
                // Ground is darker
                sr.setColor(0.25f, 0.25f, 0.3f, 1f);
            } else if (p.height > PLATFORM_HEIGHT + 1) {
                // Walls are slightly different color
                sr.setColor(0.35f, 0.3f, 0.4f, 1f);
            } else {
                // Normal platforms
                sr.setColor(0.3f, 0.35f, 0.45f, 1f);
            }
            sr.rect(p.x, p.y, p.width, p.height);
        }

        // Platform edges (top highlight)
        sr.setColor(0.5f, 0.55f, 0.65f, 1f);
        for (Platform p : platforms) {
            sr.rect(p.x, p.y + p.height - 2, p.width, 2);
        }

        sr.end();

        // Draw some guide text indicators using lines
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(Color.GRAY);
        // Simple grid lines for reference
        for (float gx = 0; gx < worldWidth; gx += 100) {
            sr.line(gx, 0, gx, 3);
        }
        sr.end();
    }

    // ── Collision queries (used by Player) ──────────────────

    public boolean collidesLeft(float px, float py, float pw, float ph) {
        for (Platform p : platforms) {
            if (p.height <= PLATFORM_HEIGHT + 1) continue; // skip thin platforms
            // Player's left edge touches platform's right edge
            if (px <= p.x + p.width && px >= p.x + p.width - 3f
                && py + ph > p.y && py < p.y + p.height) {
                return true;
            }
        }
        // World left edge
        return px <= 0;
    }

    public boolean collidesRight(float px, float py, float pw, float ph) {
        for (Platform p : platforms) {
            if (p.height <= PLATFORM_HEIGHT + 1) continue;
            // Player's right edge touches platform's left edge
            if (px + pw >= p.x && px + pw <= p.x + 3f
                && py + ph > p.y && py < p.y + p.height) {
                return true;
            }
        }
        // World right edge
        return px + pw >= worldWidth;
    }

    // ── Getters ─────────────────────────────────────────────

    public List<Platform> getPlatforms() { return platforms; }
    public float getWorldWidth() { return worldWidth; }
    public float getGroundY() { return groundY; }

    // ── Platform data ───────────────────────────────────────

    public static class Platform {
        public final float x, y, width, height;

        public Platform(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
