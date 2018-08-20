package pl.martapiatek.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius; // promień podstawy działa
    private int barrelLength; // długość lufy działa
    private Point barrelEnd = new Point(); // koĶńcowy punkt lufy
    private double barrelAngle; // kąt ustawienia lufy
    private Cannonball cannonball; // kula wystrzeliwana z działa
    private Paint paint = new Paint(); // obiekt uźywany do rysowania działa
    private CannonView view;

    public Cannon(int baseRadius, int barrelLength, int barrelWidth, CannonView view) {
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;
        this.view = view;

        paint.setStrokeWidth(barrelWidth);
        paint.setColor(Color.BLACK);
        align(Math.PI / 2);
    }

    //ustawia lufę pod wybranym kątem
    public void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;
        barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
        barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) + view.getScreenHeight() / 2;
    }

    // tworzenie i wystrzelenie kuli
    public void fireCannonball() {

        //składowa x prędkości
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() * Math.sin(barrelAngle));

        //składowa y prędkości
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() * Math.cos(barrelAngle));

        //promień kuli

        int radius = (int) (view.getScreenHeight() * CannonView.CANNONBALL_RADIUS_PERCENT);

        //utwórz kulę i umieść ją wewnątrz działa
        cannonball = new Cannonball(view, Color.BLACK, CannonView.CANNON_SOUND_ID, -radius, view.getScreenHeight() / 2 - radius, radius, velocityX, velocityY);

        cannonball.playSound(); // dźwięk wystrzału
    }

    //rysowanie działa
    public void draw(Canvas canvas) {

        //lufa
        canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x, barrelEnd.y, paint);

        //podstawa działa
        canvas.drawCircle(0, (int) view.getScreenHeight() / 2, (int) baseRadius, paint);
    }

    public Cannonball getCannonball() {
        return cannonball;
    }

    //usuwanie kuli z gry
    public void removeCannonball() {
        cannonball = null;
    }
}
