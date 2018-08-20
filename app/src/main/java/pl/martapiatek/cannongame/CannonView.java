package pl.martapiatek.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {

    //stałe rozgrywki
    public static final int MISS_PENALTY = 2; // liczba sekund odejmowana za trafienie w przeszkodę
    public static final int HIT_REWARD = 3; // liczba sekund odejmowana za trafienie w cel
    //stałe działa
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;
    //stałe kuli
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;
    //stałe celów
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 40;
    //stałe przeszkody
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;
    //tekst o rozmiarze 1/18 ekranu
    public static final double TEXT_SIXE_PERCENT = 1.0 / 18;
    //dźwięki
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private static final String TAG = "CannonView";
    private CannonThread cannonThread;
    private Activity activity;
    private boolean dialogIsDisplayed = false;
    //obiekty gry
    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;
    // wymiary
    private int screenWidth;
    private int screenHeight;
    //statystyki gry
    private boolean gameOver;
    private double timeLeft; // pozostały czas w sek
    private int shotsFired; // wykonane strzały
    private double totalElapsedTime; // czas od rozpoczęcia gry w sek
    private SoundPool soundPool; //odtwarza efekty dźwiękowe
    private SparseIntArray soundMap;

    private Paint textPaint;
    private Paint backgroundPaint;


    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;

        //zarejestruj abiekt nasłuchujący SurfaceHolder.Callback
        getHolder().addCallback(this);


        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        soundMap = new SparseIntArray(3);
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeight = h;

        textPaint.setTextSize((int) (TEXT_SIXE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    public void newGame() {

        cannon = new Cannon((int) (CANNON_BASE_RADIUS_PERCENT * screenHeight), (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth), (int) (CANNON_BARREL_WIDTH_PERCENT * screenWidth), this);

        Random random = new Random();
        targets = new ArrayList<>();

        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) * screenHeight);

        for (int n = 0; n < TARGET_PIECES; n++) {

            //losowa prędkość celu
            double velocity = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MAX_SPEED_PERCENT);

            //zmiana kolorów celu
            int color = (n % 2 == 0) ? getResources().getColor(R.color.dark, getContext().getTheme()) : getResources().getColor(R.color.light, getContext().getTheme());

            velocity = velocity * (-1); //odwróć prędkość kolejnego celu

            //utwórz nowy cel
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY, (int) (TARGET_WIDTH_PERCENT * screenWidth), (int) (TARGET_LENGTH_PERCENT * screenHeight), (int) velocity));


            targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth;

        }

        //utwórz nową przeszkodę
        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY, (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (int) (BLOCKER_SPEED_PERCENT * screenHeight)
        );

        timeLeft = 10; // rozpocznij odliczanie 10 sekund

        shotsFired = 0; //początkowa liczba strzałów
        totalElapsedTime = 0.0; //wyzeruj czas

        //uruchom nową grę po zakończeniu poprzedniej
        if (gameOver) {
            gameOver = false;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }
        hideSystemBars();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE

            );
    }

    //aktualizacja położenia elementów gry
    public void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0; //zamiana ms na s

        //jeśli kula na ekranie to aktualizuj położenie
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval);// aktualizuj położenie przeszkody

        for (GameElement target : targets)
            target.update(interval); // aktualizuj położenie celów

        timeLeft -= interval;


        //po upływie czasu
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true;
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.lose);
        }

        //wszystkie cele trafione
        if (targets.isEmpty()) {
            gameOver = true;
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.win);
        }
    }

    //ustawienie działa i wystrzał
    public void alignAndFireCannonball(MotionEvent event) {

        //miejsce dotknięcia
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        //odległość od dotknięcia do środka ekranu
        double centerMinusY = screenHeight / 2 - touchPoint.y;

        double angle = 0;

        // kąt między lufą a osią x
        angle = Math.atan2(touchPoint.x, centerMinusY);

        //skieruj działo w kierunku dotknięcia
        cannon.align(angle);

        //wystrzel kulę
        if (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
    }

    //AlertDialog po zakończeniu gry
    private void showGameOverDialog(final int messageId) {

        final DialogFragment gameResult = new DialogFragment() {
            @Override
            public Dialog onCreateDialog(Bundle bundle) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(messageId));

                //liczba strzałów i czas gry
                builder.setMessage(getResources().getString(R.string.result_format, shotsFired, totalElapsedTime));

                builder.setPositiveButton(R.string.reset_game, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialogIsDisplayed = false;
                        newGame();
                    }
                });
                return builder.create();
            }

        };

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showSystemBars();
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false);
                        gameResult.show(activity.getFragmentManager(), "results");
                    }
                }


        );


    }


    public void drawGameElements(Canvas canvas) {

        //wyczyść tło
        canvas.drawRect(0, 0, canvas.getWidth(), getHeight(), backgroundPaint);

        canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

        cannon.draw(canvas);

        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        blocker.draw(canvas);

        for (GameElement target : targets)
            target.draw(canvas);
    }

    //zderzenie kuli z celem
    public void testForCollisions() {

        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {

            for (int n = 0; n < targets.size(); n++) {

                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound();// odtwórz dźwięk
                    //dodaj czas nagrodę za trafienie w cel
                    timeLeft += targets.get(n).getHitReward();

                    cannon.removeCannonball(); // usuś kulę
                    targets.remove(n);//usuń trafiony cel

                    --n;
                    break;


                }

            }
        } else {
            cannon.removeCannonball();
        }

        //sprawdź czy kula zderza się z przeszkodą
        if (cannon.getCannonball() != null && cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound();
            cannon.getCannonball().reverseVelocityX();

            //odejmij czas za trafienie w przeszkodę
            timeLeft -= blocker.getMissPenalty();
        }
    }

    //zatrzymanie gry
    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false);
    }

    public void releaseResources() {
        soundPool.release();
        soundPool = null;
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

            );
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (!dialogIsDisplayed) {
            newGame(); // uruchom grę
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start();


        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        cannonThread.setRunning(false);

        while (retry) {
            try {
                cannonThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e(TAG, "Przerwanie wątku", e);
            }
        }


    }

    //obsługa dotyku
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();

        //użytkownik dotknął ekran lub przeciągnął palcem
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {

            //wystrzel kulę w kierunku punktu dotyku
            alignAndFireCannonball(event);
        }
        return true;
    }

    //klasa wątku sterująca pętlą gry
    private class CannonThread extends Thread {

        private SurfaceHolder surfaceHolder;
        private boolean threadIsRunning = true;


        public CannonThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
            setName("CannonThread");
        }

        //zmienia stan wykonywania
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        //steruje pętlą gry


        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);

                    synchronized (surfaceHolder) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS);
                        testForCollisions();
                        drawGameElements(canvas);
                        previousFrameTime = currentTime;
                    }

                } finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
