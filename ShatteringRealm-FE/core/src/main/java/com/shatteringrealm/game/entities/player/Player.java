package com.shatteringrealm.game.entities.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.shatteringrealm.game.world.GameWorld;

// Sementara menggunakan kotak sebagai karakter

public class Player {

    // ── Dimensions ──────────────────────────────────────────────
    public static final float WIDTH  = 14f;
    public static final float HEIGHT = 28f;

    // ── Horizontal movement ─────────────────────────────────────
    private static final float RUN_SPEED       = 160f;   // max horizontal speed
    private static final float ACCELERATION    = 1200f;  // ground accel
    private static final float DECELERATION    = 1600f;  // ground friction
    private static final float AIR_ACCEL       = 800f;   // air control
    private static final float AIR_DECEL       = 400f;

    // ── Jumping ─────────────────────────────────────────────────
    private static final float JUMP_VELOCITY     = 400f;   // initial upward speed
    private static final float GRAVITY           = 900f;   // downward pull
    private static final float MAX_FALL_SPEED    = 500f;
    private static final float FAST_FALL_SPEED   = 600f;
    private static final float JUMP_CUT_MULT     = 0.4f;   // multiplier when releasing jump early
    private static final float COYOTE_TIME       = 0.08f;  // seconds you can still jump after leaving ground
    private static final float JUMP_BUFFER_TIME  = 0.1f;   // seconds a jump press is remembered

    // ── Wall interactions ───────────────────────────────────────
    private static final float WALL_SLIDE_SPEED  = 60f;
    private static final float WALL_JUMP_X       = 180f;
    private static final float WALL_JUMP_Y       = 260f;
    private static final float WALL_JUMP_LOCK    = 0.12f;  // seconds of forced horizontal movement

    // ── Dash ────────────────────────────────────────────────────
    private static final float DASH_SPEED    = 350f;
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_COOLDOWN = 0.6f;

    // ── Roll ────────────────────────────────────────────────────
    private static final float ROLL_SPEED    = 200f;
    private static final float ROLL_DURATION = 0.35f;
    private static final float ROLL_COOLDOWN = 0.5f;

    // ── State ───────────────────────────────────────────────────
    private float x, y;
    private float vx, vy;
    private boolean grounded;
    private boolean facingRight = true;

    // Coyote & jump buffer
    private float coyoteTimer;
    private float jumpBufferTimer;

    // Wall
    private boolean touchingWallLeft, touchingWallRight;
    private boolean wallSliding;
    private float wallJumpLockTimer;
    private int wallJumpDir; // -1 or 1

    // Dash
    private boolean dashing;
    private float dashTimer;
    private float dashCooldownTimer;
    private int dashDir;

    // Roll
    private boolean rolling;
    private float rollTimer;
    private float rollCooldownTimer;
    private int rollDir;

    public Player(float startX, float startY) {
        this.x = startX;
        this.y = startY;
    }

    // ── INPUT ───────────────────────────────────────────────────

    public void handleInput() {
        // Horizontal input
        int inputX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  inputX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) inputX += 1;

        if (inputX != 0 && wallJumpLockTimer <= 0 && !dashing && !rolling) {
            facingRight = inputX > 0;
        }

        // Store raw input for update
        this.inputX = inputX;

        // Jump (buffered)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            jumpBufferTimer = JUMP_BUFFER_TIME;
        }

        // Jump cut (variable height)
        if (!(Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.W)
              || Gdx.input.isKeyPressed(Input.Keys.UP))) {
            jumpHeld = false;
        } else {
            jumpHeld = true;
        }

        // Fast fall
        fastFallPressed = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        // Dash
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldownTimer <= 0 && !rolling) {
            dashRequested = true;
        }

        // Roll
        if (Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT) && rollCooldownTimer <= 0 && grounded && !dashing) {
            rollRequested = true;
        }
    }

    // Raw input state (set by handleInput, consumed by update)
    private int inputX;
    private boolean jumpHeld;
    private boolean fastFallPressed;
    private boolean dashRequested;
    private boolean rollRequested;

    // ── UPDATE ──────────────────────────────────────────────────

    public void update(float dt, GameWorld world) {
        // Tick cooldowns
        if (dashCooldownTimer > 0) dashCooldownTimer -= dt;
        if (rollCooldownTimer > 0) rollCooldownTimer -= dt;
        if (jumpBufferTimer > 0)   jumpBufferTimer -= dt;
        if (wallJumpLockTimer > 0) wallJumpLockTimer -= dt;

        // ── DASH ────────────────────────────────────────────
        if (dashRequested && !dashing) {
            dashing = true;
            dashTimer = DASH_DURATION;
            dashCooldownTimer = DASH_COOLDOWN;
            dashDir = facingRight ? 1 : -1;
            vy = 0; // freeze vertical
            dashRequested = false;
        }

        if (dashing) {
            dashTimer -= dt;
            vx = dashDir * DASH_SPEED;
            vy = 0; // no gravity during dash
            if (dashTimer <= 0) {
                dashing = false;
                vx = dashDir * RUN_SPEED; // carry momentum
            }
        }

        // ── ROLL ────────────────────────────────────────────
        if (rollRequested && !rolling && grounded) {
            rolling = true;
            rollTimer = ROLL_DURATION;
            rollCooldownTimer = ROLL_COOLDOWN;
            rollDir = facingRight ? 1 : -1;
            rollRequested = false;
        }

        if (rolling) {
            rollTimer -= dt;
            vx = rollDir * ROLL_SPEED;
            if (rollTimer <= 0 || !grounded) {
                rolling = false;
            }
        }

        // ── HORIZONTAL MOVEMENT ─────────────────────────────
        if (!dashing && !rolling) {
            float accel = grounded ? ACCELERATION : AIR_ACCEL;
            float decel = grounded ? DECELERATION : AIR_DECEL;

            if (wallJumpLockTimer > 0) {
                // During wall jump lock, force horizontal direction
                vx = wallJumpDir * WALL_JUMP_X;
            } else if (inputX != 0) {
                vx += inputX * accel * dt;
                if (Math.abs(vx) > RUN_SPEED) vx = Math.signum(vx) * RUN_SPEED;
            } else {
                // Decelerate to zero
                if (vx > 0) {
                    vx -= decel * dt;
                    if (vx < 0) vx = 0;
                } else if (vx < 0) {
                    vx += decel * dt;
                    if (vx > 0) vx = 0;
                }
            }
        }

        // ── WALL DETECTION ──────────────────────────────────
        touchingWallLeft  = world.collidesLeft(x, y, WIDTH, HEIGHT);
        touchingWallRight = world.collidesRight(x, y, WIDTH, HEIGHT);
        wallSliding = !grounded && ((touchingWallLeft && inputX < 0) || (touchingWallRight && inputX > 0));

        // ── GRAVITY & VERTICAL ──────────────────────────────
        if (!dashing) {
            if (wallSliding) {
                // Slow slide down walls
                if (vy < 0) {
                    vy = Math.max(vy, -WALL_SLIDE_SPEED);
                } else {
                    vy -= GRAVITY * dt;
                    if (vy < -WALL_SLIDE_SPEED) vy = -WALL_SLIDE_SPEED;
                }
            } else {
                vy -= GRAVITY * dt;

                // Fast fall
                float maxFall = fastFallPressed ? FAST_FALL_SPEED : MAX_FALL_SPEED;
                if (vy < -maxFall) vy = -maxFall;
            }

            // Jump cut: if player releases jump while ascending, reduce upward velocity
            if (!jumpHeld && vy > 0) {
                vy *= JUMP_CUT_MULT;
            }
        }

        // ── COYOTE TIME ─────────────────────────────────────
        if (grounded) {
            coyoteTimer = COYOTE_TIME;
        } else {
            coyoteTimer -= dt;
        }

        // ── JUMP ────────────────────────────────────────────
        if (jumpBufferTimer > 0) {
            if (coyoteTimer > 0) {
                // Normal jump
                vy = JUMP_VELOCITY;
                grounded = false;
                coyoteTimer = 0;
                jumpBufferTimer = 0;
            } else if (wallSliding || touchingWallLeft || touchingWallRight) {
                // Wall jump
                vy = WALL_JUMP_Y;
                wallJumpDir = touchingWallLeft ? 1 : -1;
                wallJumpLockTimer = WALL_JUMP_LOCK;
                facingRight = wallJumpDir > 0;
                grounded = false;
                jumpBufferTimer = 0;
            }
        }

        // ── APPLY VELOCITY ──────────────────────────────────
        x += vx * dt;
        y += vy * dt;

        // ── COLLISION WITH WORLD ────────────────────────────
        // Ground collision
        grounded = false;
        for (GameWorld.Platform p : world.getPlatforms()) {
            if (isOnTopOf(p)) {
                y = p.y + p.height;
                vy = 0;
                grounded = true;
                break;
            }
        }

        // Wall collision (prevent going through)
        for (GameWorld.Platform p : world.getPlatforms()) {
            if (overlapsHorizontally(p)) {
                if (vx > 0) {
                    x = p.x - WIDTH;
                } else if (vx < 0) {
                    x = p.x + p.width;
                }
            }
        }

        // Ceiling collision
        for (GameWorld.Platform p : world.getPlatforms()) {
            if (hitsBottom(p)) {
                y = p.y - HEIGHT;
                vy = 0;
            }
        }

        // World bounds
        if (x < 0) { x = 0; vx = 0; }
        if (x + WIDTH > world.getWorldWidth()) { x = world.getWorldWidth() - WIDTH; vx = 0; }

        // Fell off the world? Reset
        if (y < -100) {
            x = 100;
            y = world.getGroundY() + GameWorld.PLATFORM_HEIGHT + 10;
            vx = 0;
            vy = 0;
        }
    }

    // ── Collision helpers ────────────────────────────────────

    private boolean isOnTopOf(GameWorld.Platform p) {
        return vy <= 0
            && x + WIDTH > p.x && x < p.x + p.width           // horizontal overlap
            && y >= p.y + p.height - 2f && y <= p.y + p.height + 4f  // feet near top
            && y + vy * Gdx.graphics.getDeltaTime() <= p.y + p.height; // was above or at
    }

    private boolean overlapsHorizontally(GameWorld.Platform p) {
        // Check if player's body overlaps with a wall portion of a platform
        float feetMargin = 4f;
        return y + feetMargin < p.y + p.height && y + HEIGHT > p.y  // vertical overlap (not just on top)
            && x + WIDTH > p.x && x < p.x + p.width;
    }

    private boolean hitsBottom(GameWorld.Platform p) {
        return vy > 0
            && x + WIDTH > p.x && x < p.x + p.width
            && y + HEIGHT >= p.y && y + HEIGHT <= p.y + 4f;
    }

    // ── RENDER ──────────────────────────────────────────────

    public void render(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Body color based on state
        if (dashing) {
            sr.setColor(Color.CYAN);
        } else if (rolling) {
            sr.setColor(Color.YELLOW);
        } else if (wallSliding) {
            sr.setColor(Color.ORANGE);
        } else if (!grounded) {
            sr.setColor(Color.SKY);
        } else {
            sr.setColor(Color.WHITE);
        }

        float drawHeight = rolling ? HEIGHT * 0.5f : HEIGHT;
        float drawY = y;

        sr.rect(x, drawY, WIDTH, drawHeight);

        // Eyes to show facing direction
        sr.setColor(Color.RED);
        float eyeX = facingRight ? x + WIDTH - 4f : x + 1f;
        float eyeY = drawY + drawHeight - 6f;
        sr.rect(eyeX, eyeY, 3f, 3f);

        sr.end();
    }

    // ── Getters ─────────────────────────────────────────────

    public float getX() { return x + WIDTH / 2f; }
    public float getY() { return y; }
    public boolean isGrounded() { return grounded; }
    public boolean isDashing() { return dashing; }
    public boolean isRolling() { return rolling; }
}
