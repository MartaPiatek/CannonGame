package pl.martapiatek.cannongame;

public class Target extends GameElement {

    private int hitReward; // nagroda za trafienie w cel

    public Target(CannonView view, int color, int hitReward, int x, int y, int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY);
        this.hitReward = hitReward;
    }

    public int getHitReward() {
        return hitReward;
    }
}
