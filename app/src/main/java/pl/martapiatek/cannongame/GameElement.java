package pl.martapiatek.cannongame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {

    protected CannonView view; // widok, w którym znajduje się GameElement
    protected Paint paint = new Paint(); // obiekt rysujący GameElement
    protected Rect shape; // prostokątne granice GameElement
    protected float velocityY; // prędkość ruchu GameElement w osi pionowej
    private int soundId; // dźwięk powiązany z GameElement

    public GameElement(CannonView view, int color, int soundId, int x, int y, int width, int length, float velocityY) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length);
        this.soundId = soundId;
        this.velocityY = velocityY;
    }

    //aktualizuj położenie GameElement i sprawdź czy uderzył w ścianę
    public void update(double interval) {

        shape.offset(0, (int) (velocityY * interval));

        //zmień kierunek ruchu po zderzeniu ze ścianą
        if (shape.top < 0 && velocityY < 0 || shape.bottom > view.getScreenHeight() && velocityY > 0)
            velocityY *= -1;
    }

    //rysuje GameElement na danym obiekcie Canvas
    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    //odtwarza dźwięk dla danego elementy GameElement
    public void playSound() {
        view.playSound(soundId);
    }
}
