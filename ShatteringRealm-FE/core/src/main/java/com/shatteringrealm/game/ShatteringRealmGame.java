package com.shatteringrealm.game;

import com.badlogic.gdx.Game;
import com.shatteringrealm.game.ui.screens.GameScreen;

// Main file

public class ShatteringRealmGame extends Game {

    @Override
    public void create() {
        // Tambahin menu: MenuScreen, LevelSelectScreen, GameOverScreen, etc.
        setScreen(new GameScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
