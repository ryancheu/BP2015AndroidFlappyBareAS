package com.example.flappybird;

import java.util.ArrayList;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    /** How much vertical space (in pixels) there is between pipes **/
    private final int PIPE_OPENING_HEIGHT = 350;
    /** How wide (in pixels) the pipes are **/
    private final int PIPE_WIDTH = 100;
    /** Distance between pipes (in pixels) **/
    private final int DIST_BETWEEN_PIPE = 500;

    /** Width of player box in pixels **/
    private final int PLAYER_WIDTH = 100;
    /** Height of player box in pixels **/
    private final int PLAYER_HEIGHT = 70;
    /** Offset from left side of the screen of the player box **/
    private final int PLAYER_OFFSET = 20;

    /** This is the start x location, pipes start spawning at location 0 **/
    private final int PLAYER_START_POSITION = -1000;

    /** Player's y position, pixels from the top of the screen **/
    private float playerY = 20;

    /**
     * How far the player has traveled. Starts negative so that we don't have a
     * pipe right away
     **/
    private float distanceTraveled = PLAYER_START_POSITION;

    /** the change, in pixels, of the player Y position per second **/
    private float playerYVel = 10;

    /** the paintbrush we use **/
    private Paint paint;

    /**
     * Last time the frame was drawn, in milliseconds since Thursday, 1 January
     * 1970 (UTC)
     **/
    private long lastFrame = -1;

    /** Width of the screen in pixels **/
    private int screenWidth;
    /** Height of the screen in pixels **/
    private int screenHeight;

    /** This is the bounding box of the player (bird) **/
    private RectF playerRect;

    /** List of all pipe heights, generated as we go **/
    private ArrayList<Integer> pipeHeights = new ArrayList<Integer>();

    /** Random number generator **/
    private Random random;

    private int nextPipeToPass = 0;

    private Bitmap birdBitmap;
    private Bitmap backgroundBitmap;
    private Bitmap topPipeBitmap;
    private Bitmap bottomPipeBitmap;

    private MediaPlayer flapPlayer;

    /** If true, don't move the player **/
    private boolean paused = false;

    public GameView(Context context) {
        super(context);
        flapPlayer = MediaPlayer.create(getContext(),
                R.raw.sfx_wing);
        paint = new Paint();
        birdBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.bird);
        backgroundBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.background);
        topPipeBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.obstacle_top);

        bottomPipeBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.obstacle_bottom);



        // We seed the random number generator with current time so that the
        // pipes always start at different positions.
        random = new Random(System.currentTimeMillis());
    }

    /**
     * This code is called once when the screen is first shown, and whenever the
     * screen is resized (change orientation)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenHeight = h;
        screenWidth = w;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*
         * This is our main game loop, it performs both drawing and updating.
         * 
         * Usually, it's advised that you multi-thread this code, but it's far
         * simpler this way.
         */

        // Draw everything on the screen
        drawBackground(canvas);
        drawPlayer(canvas);
        drawPipes(canvas);
        drawScore(canvas);

        // If the game is not paused, update the game state
        if (!paused) {
            update();
            checkCollisions();
        }

        // This call triggers the screen to redraw (basically call onDraw
        // again).
        invalidate();
    }

    /**
     * Draw the pipes on screen.
     * 
     * This function is complete.
     */
    private void drawPipes(Canvas canvas) {

        for (int index : getPipesOnScreen()) {
            //drawPipe(index, isTop, canvas)
            drawPipe(index, true, canvas);
            drawPipe(index, false, canvas);
        }

    }

    /**
     * @return a list of pipe indexes that are currently on screen
     */
    private ArrayList<Integer> getPipesOnScreen() {

        // TODO: Replace lowIndex and highIndex with better bounds
        final int lowIndex = ((int)distanceTraveled - screenWidth)
                / (PIPE_WIDTH + DIST_BETWEEN_PIPE);
        final int highIndex = ((int) distanceTraveled + 2 * screenWidth)
                / (PIPE_WIDTH + DIST_BETWEEN_PIPE);
        ArrayList<Integer> returnPipes = new ArrayList<Integer>();
        for (int i = lowIndex; i < highIndex; i++) {
            if (i >= 0) {
                returnPipes.add(i);
            }
        }
        return returnPipes;
    }

    /**
     * This is the x-coordinate of the left side of a pipe
     */
    private int getPipeLeft(int index) {
        // TODO: Vary this with the distanceTraveled
        // of the player, pipe index
        // 0 * (PIPE_WIDTH + DIST_BETWEEN)
        // 1 * "
        // 2 * "
        //            absolute position                   - playerPosition
        return (index * (PIPE_WIDTH + DIST_BETWEEN_PIPE)) - (int)distanceTraveled;
    }

    /**
     * This is is the y-coordinate of the bottom of the top pipe
     */
    private int getPipeHeight(int index) {
        if (pipeHeights.size() - 1 < index) {
            for ( int i = pipeHeights.size() - 1; i < index; i++ ) {
                pipeHeights.add(random.nextInt
                        (screenHeight - PIPE_OPENING_HEIGHT));
            }
        }
        return pipeHeights.get(index);
    }

    /**
     * Given a specific pipe number, return the box that encloses the pipe
     * 
     * @param isTop
     *            if true, return the top pipe, otherwise return the bottom pipe
     * @return
     */
    private RectF getPipeBoundingBox(int index, boolean isTop) {
        int left = getPipeLeft(index);
        int height = getPipeHeight(index);
        if (isTop) {
            return new RectF(left, 0, left + PIPE_WIDTH, height);
        } else {
            return new RectF(left, height + PIPE_OPENING_HEIGHT, left
                    + PIPE_WIDTH, screenHeight);
        }

    }

    private void drawPipe(int index, boolean isTopPipe, Canvas canvas) {
        // TODO: Draw a single pipe, either top or bottom
        // depending on value of isTopPipe
        // paint.setColor(Color.GREEN);
        RectF pipeBox = getPipeBoundingBox(index, isTopPipe);
        if ( isTopPipe ) {
            canvas.drawBitmap(topPipeBitmap, null, pipeBox, paint);
        }
        else {
            canvas.drawBitmap(bottomPipeBitmap, null, pipeBox, paint);
        }

    }

    /**
     * Draw the player
     */
    private void drawPlayer(Canvas canvas) {
        float top = playerY;
        float bottom = top + PLAYER_HEIGHT;
        float left = PLAYER_OFFSET;
        float right = left + PLAYER_WIDTH;

        playerRect = new RectF(left, top, right, bottom);

        // paint.setColor(Color.YELLOW);
        canvas.drawBitmap(birdBitmap, null, playerRect, paint);
        // canvas.drawRect(playerRect, paint);

        // TODO: Draw the player inside the rectangle
    }

    private void drawScore(Canvas canvas) {
        paint.setTextSize(40);
        paint.setColor(Color.BLACK);
        canvas.drawText("score: " + nextPipeToPass, 40, 40, paint);
    }

    private void drawBackground(Canvas canvas) {
        RectF screenRect = new RectF(0,0,screenWidth,
                screenHeight);
        canvas.drawBitmap(backgroundBitmap,
                null, screenRect, paint);
        // TODO: Draw the background on the screen
    }

    /**
     * Collision detection, collide with all possible pipes and the top/bottom
     * of the screen.
     **/
    private void checkCollisions() {
        // TODO: check if the player has collided with the floor or ceiling
        if ( playerY < 0 || playerY > (screenHeight - PLAYER_HEIGHT)) {
            gameOver();
        }

        for (int index : getPipesOnScreen()) {
            // These are bounding boxes for the top and bottom pipes
            RectF topPipe = getPipeBoundingBox(index, true);

            RectF bottomPipe = getPipeBoundingBox(index, false);

            if ( RectF.intersects(playerRect, topPipe)
                    || RectF.intersects(playerRect, bottomPipe)) {
                gameOver();
            }

            // TODO: check if any of the pipes collide with player
        }
    }

    private void gameOver() {
        paused = true;
        showScoreDialog(nextPipeToPass);
        // TODO: reset game, show score screen
    }

    /*
     * Reset to starting location
     */
    private void reset() {
        lastFrame = -1;
        playerY = screenHeight/2;
        playerYVel = 0;
        distanceTraveled = PLAYER_START_POSITION;
        pipeHeights = new ArrayList<Integer>();
        nextPipeToPass = 0;
        // TODO: set variables to initial values
    }

    /**
     * This function is called whenever the player touches the screen.
     * ACTION_DOWN means the player just touched the screen.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            playerYVel = -400;
            flapPlayer.seekTo(0);
            flapPlayer.start();
            // TODO: make player go up, play sound
            break;
        }
        return true;
    }

    /**
     * This is the main update code, it updates the player position based on his
     * velocity, and adds gravity as well.
     */
    private void update() {

        /*
         * We keep track of the current and previous frame times to ensure that
         * speed is kept constant regardless of the FPS.
         */
        long currentTime = System.currentTimeMillis();
        if (lastFrame != -1) {
            long diff = currentTime - lastFrame;
            float mult = ((float) diff / 1000f);
            playerYVel += mult * 500;
            playerY = playerY + (playerYVel * mult);

            //distanceTraveled = (float)distanceTraveled + (float)(mult * 200);
            //-4 + 0.5 -> -3.5 -> -3
            //0 + 0.5 -> 0.5 -> 0
            distanceTraveled  += (mult * 200.0);

            int passPipeRightEdge = getPipeLeft(nextPipeToPass) + PIPE_WIDTH;
            if ( passPipeRightEdge < PLAYER_OFFSET) {
                nextPipeToPass += 1;
            }


            // TODO: move player, add gravity, check to see if we've passed any
            // pipes and update score if we have
        }
        lastFrame = currentTime;
    }

    private void showScoreDialog(int score) {

        /*
         * This creates a pop up with the score and a button to play again
         */
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage("Your score was: " + score).setTitle("Game over!");

        builder.setPositiveButton("Play Again",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        paused = false;
                        reset();
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();

        dialog.show();

        // TODO: show the pop up
    }
}
