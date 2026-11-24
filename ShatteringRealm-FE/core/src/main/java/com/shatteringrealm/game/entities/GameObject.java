package com.shatteringrealm.game.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Abstract base class for every entity in Shattering Realm.
 *
 * <p>What lives here (truly universal to ALL game objects):</p>
 * <ul>
 *   <li>Position (x, y) and dimensions (width, height)</li>
 *   <li>Velocity (vx, vy) — raw movement vector, NOT gravity/physics</li>
 *   <li>HP and active/destroyed lifecycle</li>
 *   <li>Hurtbox — the area where this entity can receive damage</li>
 *   <li>Facing direction — left/right for sprite flipping</li>
 *   <li>Animation/sprite rendering with facing-aware flip</li>
 * </ul>
 *
 * <p>What does NOT live here:</p>
 * <ul>
 *   <li>Gravity and platform collision → separate PhysicsBody component</li>
 *   <li>Hitbox (attack area) → belongs on weapon/attack classes</li>
 *   <li>AI, movement patterns, shooting → belongs on entity subclasses</li>
 * </ul>
 *
 * <p>Inheritance hierarchy plan:</p>
 * <pre>
 *   GameObject (this class)
 *   ├── LivingEntity (Player, Enemy, Boss — has combat stats, knockback)
 *   ├── Projectile (bullets, thrown weapons — has damage, owner, lifetime)
 *   ├── Pickup (health drops, buff orbs, boss rewards)
 *   └── EnvironmentObject (breakable crates, traps, interactables)
 * </pre>
 */
public abstract class GameObject {

    // ══════════════════════════════════════════════════════════════
    //  POSITION & DIMENSIONS
    // ══════════════════════════════════════════════════════════════

    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected float speed;
    protected Color color;

    // ══════════════════════════════════════════════════════════════
    //  VELOCITY — raw movement vector; gravity is NOT applied here.
    //  Entities that need physics will use a PhysicsBody component.
    // ══════════════════════════════════════════════════════════════

    protected float vx;
    protected float vy;

    // ══════════════════════════════════════════════════════════════
    //  LIFECYCLE — every entity can be active or destroyed
    // ══════════════════════════════════════════════════════════════

    protected boolean active = true;

    // ══════════════════════════════════════════════════════════════
    //  HP — universal: Player ~100-200, enemies vary, projectiles
    //  have minimal HP (1-2) so the player can parry them.
    //  Environmental objects use HP for destructibility.
    // ══════════════════════════════════════════════════════════════

    protected int hp;
    protected int maxHp;

    // ══════════════════════════════════════════════════════════════
    //  FACING DIRECTION — essential for a side-scroller.
    //  true = facing right (default), false = facing left.
    //  Used to flip sprites and determine attack direction.
    // ══════════════════════════════════════════════════════════════

    protected boolean facingRight = true;

    // ══════════════════════════════════════════════════════════════
    //  ANIMATION & SPRITE
    //  Base rendering: sprite or animation with automatic flipping
    //  based on facingRight. Subclasses override render() for
    //  specialized effects (dash trails, damage flash, etc.).
    // ══════════════════════════════════════════════════════════════

    protected TextureRegion sprite;
    protected TextureRegion shapeRenderer;
    protected Animation<TextureRegion> currentAnimation;
    protected float stateTime = 0f;

    // ══════════════════════════════════════════════════════════════
    //  HURTBOX — the area on THIS entity where it can receive
    //  damage. By default, matches the entity's full bounds.
    //  Override getHurtbox() to customize (e.g., smaller core area
    //  for the player, larger for big bosses).
    //
    //  Note: Hitbox (the area that DEALS damage) belongs on
    //  weapon/attack/projectile classes, not here.
    // ══════════════════════════════════════════════════════════════

    // Reusable rectangle to avoid GC pressure during collision checks
    protected final Rectangle hurtboxRect = new Rectangle();

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTORS
    // ══════════════════════════════════════════════════════════════

    /**
     * Minimal constructor for objects that don't need HP
     * (e.g., purely decorative environmental objects).
     */
    protected GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hp = 1;
        this.maxHp = 1;
    }

    /**
     * Full constructor for entities with HP.
     */
    protected GameObject(float x, float y, float width, float height, int hp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.hp = hp;
        this.maxHp = hp;
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE — called every frame by the game loop
    // ══════════════════════════════════════════════════════════════

    /**
     * Base update: advances animation timer and applies velocity.
     * Subclasses should call super.update(delta) first, then add
     * their own logic (AI, input, physics, etc.).
     */
    public void update(float delta) {
        if (!active) return;

        // Advance animation clock
        stateTime += delta;

        // Apply velocity to position
        x += vx * delta;
        y += vy * delta;
    }

    // ══════════════════════════════════════════════════════════════
    //  RENDER — base sprite/animation rendering with auto-flip
    // ══════════════════════════════════════════════════════════════

    /**
     * Renders the entity using SpriteBatch. Automatically flips
     * the sprite horizontally based on facingRight.
     *
     * <p>Subclasses override this for custom rendering (e.g., the
     * Player might render dash afterimages, bosses might render
     * phase-specific auras).</p>
     */
    public void render(SpriteBatch batch) {
        if (!active || batch == null) return;

        TextureRegion frame = getCurrentFrame();
        if (frame == null) return;

        // Flip handling: if facingRight is false, flip the texture
        // by drawing with negative width from the right edge.
        if (facingRight) {
            batch.draw(frame, x, y, width, height);
        } else {
            batch.draw(frame, x + width, y, -width, height);
        }
    }

    /**
     * Debug/placeholder rendering using ShapeRenderer.
     * Draws a colored rectangle so we can test without sprites.
     * Subclasses override for custom debug visuals.
     */
    public void renderDebug(ShapeRenderer sr) {
        if (!active || sr == null) return;
        sr.rect(x, y, width, height);
    }

    /**
     * Returns the current texture frame to draw.
     * Prefers animation over static sprite.
     */
    protected TextureRegion getCurrentFrame() {
        if (currentAnimation != null) {
            return currentAnimation.getKeyFrame(stateTime, true);
        }
        return sprite;
    }

    // ══════════════════════════════════════════════════════════════
    //  COLLISION — Hurtbox
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns the hurtbox — the area where this entity can receive
     * damage. By default, matches the entity's full bounding box.
     *
     * <p>Override in subclasses to customize. For example:</p>
     * <ul>
     *   <li>Player: slightly smaller than sprite for forgiving hits</li>
     *   <li>Boss: matches the visible body, not the full sprite</li>
     * </ul>
     *
     * <p>Returns the same Rectangle instance each call (mutated in
     * place) to avoid garbage collection pressure during the
     * collision phase.</p>
     */
    public Rectangle getHurtbox() {
        hurtboxRect.set(x, y, width, height);
        return hurtboxRect;
    }

    /**
     * Returns the physics/world bounding box for platform and wall
     * collision. By default same as full bounds. Override if the
     * collision shape differs from the visual (e.g., the player's
     * feet are narrower than the sprite).
     */
    public Rectangle getBounds() {
        // Reuses hurtboxRect temporarily — callers should read
        // immediately or copy, not hold a reference across frames.
        return new Rectangle(x, y, width, height);
    }

    // ══════════════════════════════════════════════════════════════
    //  DAMAGE & LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    /**
     * Inflicts damage on this entity. Returns true if the entity
     * was destroyed by this hit (HP reached 0).
     *
     * <p>Subclasses can override to add: knockback, invincibility
     * frames, damage flash, death animation trigger, etc.</p>
     */
    public boolean takeDamage(int damage) {
        if (!active || damage <= 0) return false;

        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            destroy();
            return true;
        }
        return false;
    }

    /**
     * Heals the entity, clamped to maxHp.
     */
    public void heal(int amount) {
        if (!active || amount <= 0) return;
        hp = Math.min(hp + amount, maxHp);
    }

    /**
     * Marks this entity as destroyed/inactive.
     * Override to trigger death animations, loot drops, etc.
     */
    public void destroy() {
        this.active = false;
    }

    public boolean isDestroyed() {
        return !active;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAlive() {
        return active && hp > 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  POSITION & MOVEMENT GETTERS/SETTERS
    // ══════════════════════════════════════════════════════════════

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    /** Returns the horizontal center of the entity. */
    public float getCenterX() { return x + width / 2f; }

    /** Returns the vertical center of the entity. */
    public float getCenterY() { return y + height / 2f; }

    /** Sets position so the entity is centered on (cx, cy). */
    public void setCenter(float cx, float cy) {
        this.x = cx - width / 2f;
        this.y = cy - height / 2f;
    }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = Math.max(0, width); }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = Math.max(0, height); }

    public float getVx() { return vx; }
    public void setVx(float vx) { this.vx = vx; }

    public float getVy() { return vy; }
    public void setVy(float vy) { this.vy = vy; }

    /** Convenience: set both velocity components at once. */
    public void setVelocity(float vx, float vy) {
        this.vx = vx;
        this.vy = vy;
    }

    // ══════════════════════════════════════════════════════════════
    //  FACING DIRECTION
    // ══════════════════════════════════════════════════════════════

    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }

    /** Convenience: face toward a target x position. */
    public void faceToward(float targetX) {
        this.facingRight = targetX > getCenterX();
    }

    // ══════════════════════════════════════════════════════════════
    //  HP GETTERS/SETTERS
    // ══════════════════════════════════════════════════════════════

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = Math.max(0, Math.min(hp, maxHp)); }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) {
        this.maxHp = Math.max(1, maxHp);
        if (hp > this.maxHp) hp = this.maxHp;
    }

    /** Returns HP as a 0.0–1.0 fraction, useful for health bars. */
    public float getHpPercent() {
        return maxHp > 0 ? (float) hp / maxHp : 0f;
    }

    // ══════════════════════════════════════════════════════════════
    //  ANIMATION & SPRITE
    // ══════════════════════════════════════════════════════════════

    public TextureRegion getSprite() { return sprite; }
    public void setSprite(TextureRegion sprite) { this.sprite = sprite; }

    public Animation<TextureRegion> getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(Animation<TextureRegion> animation) {
        // Only reset stateTime if we're switching to a different animation,
        // so looping animations aren't restarted every frame.
        if (this.currentAnimation != animation) {
            this.currentAnimation = animation;
            this.stateTime = 0f;
        }
    }

    public float getStateTime() { return stateTime; }

    // ══════════════════════════════════════════════════════════════
    //  ACTIVE STATE — allows subclasses to reactivate (Object Pool)
    // ══════════════════════════════════════════════════════════════

    /**
     * Reactivates a destroyed entity (for Object Pool recycling).
     * Resets HP to max and clears state time.
     */
    public void reactivate(float x, float y) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.hp = maxHp;
        this.active = true;
        this.stateTime = 0f;
    }
}
