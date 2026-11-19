package com.shatteringrealm.game.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.shatteringrealm.game.ShatteringRealmGame;
import com.shatteringrealm.game.entities.player.Player;
import com.shatteringrealm.game.world.GameWorld;

// Main gameplay screen render

public class GameScreen extends ScreenAdapter {

    // Virtual world dimensions (in "world units", not pixels)
    public static final float WORLD_WIDTH  = 480f;
    public static final float WORLD_HEIGHT = 270f;

    private final ShatteringRealmGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;

    private Player player;
    private GameWorld world;

    public GameScreen(ShatteringRealmGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();

        world  = new GameWorld();
        player = new Player(100, world.getGroundY() + GameWorld.PLATFORM_HEIGHT);
    }

    @Override
    public void render(float delta) {
        // --- UPDATE ---
        player.handleInput();
        player.update(delta, world);

        // Camera follows player horizontally, clamped to world bounds
        float camX = Math.max(WORLD_WIDTH / 2f,
                     Math.min(player.getX(), world.getWorldWidth() - WORLD_WIDTH / 2f));
        camera.position.set(camX, WORLD_HEIGHT / 2f, 0);
        camera.update();

        // --- RENDER ---
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw the world (platforms, ground)
        world.render(shapeRenderer);

        // Draw the player
        player.render(shapeRenderer);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
