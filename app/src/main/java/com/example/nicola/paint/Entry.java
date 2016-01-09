package com.example.nicola.paint;

import android.graphics.Color;
import android.media.Image;
import android.graphics.Bitmap;


/**
 * This class is used to provide a small component that
 * the user can draw handwritten letters into. This class
 * also contains the routines necessary to crop and downsample
 * the written character.
 */
public class Entry {

    /**
     * The image that the user is drawing into.
     */
    protected Image entryImage;

    /**
     * A graphics handle to the image that the
     * user is drawing into.
     */
    protected Bitmap entryGraphics;

    /**
     * The last x that the user was drawing at.
     */
    protected int lastX = -1;

    /**
     * The last y that the user was drawing at.
     */
    protected int lastY = -1;

    /**
     * The down sample component used width this
     * component.
     */
    protected Sample sample;

    /**
     * Specifies the left boundary of the cropping
     * rectangle.
     */
    protected int downSampleLeft;

    /**
     * Specifies the right boundary of the cropping
     * rectangle.
     */
    protected int downSampleRight;

    /**
     * Specifies the top boundary of the cropping
     * rectangle.
     */
    protected int downSampleTop;

    /**
     * Specifies the bottom boundary of the cropping
     * rectangle.
     */
    protected int downSampleBottom;

    /**
     * The downsample ratio for x.
     */
    protected double ratioX;

    /**
     * The downsample ratio for y
     */
    protected double ratioY;

    /**
     * The pixel map of what the user has drawn.
     * Used to downsample it.
     */
    protected int pixelMap[];


    protected int width, height;

    /**
     * The constructor.
     */
    // TODO passare l'immagine presa dal DrawingView
    public Entry(Bitmap image) {
        height = image.getHeight();
        width = image.getWidth();
        entryGraphics = image;
    }


    /**
     * Set the sample control to use. The
     * sample control displays a downsampled
     * version of the character.
     *
     * @param s
     */
    public void setSample(Sample s) {
        sample = s;
    }

    /**
     * Get the down sample component to be used
     * width this component.
     *
     * @return The down sample component.
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * This method is called internally to
     * see if there are any pixels in the given
     * scan line. This method is used to perform
     * autocropping.
     *
     * @param y The horizontal line to scan.
     * @return True if there were any pixels in this
     * horizontal line.
     */
    protected boolean hLineClear(int y) {
        int w = width;
        for (int i = 0; i < w; i++) {
            if (pixelMap[(y * w) + i] != 0)
                return false;
        }
        return true;
    }

    /**
     * This method is called to determine ....
     *
     * @param x The vertical line to scan.
     * @return True if there are any pixels in the
     * specified vertical line.
     */
    protected boolean vLineClear(int x) {
        int w = width;
        int h = height;
        for (int i = 0; i < h; i++) {
            if (pixelMap[(i * w) + x] != 0)
                return false;
        }
        return true;
    }

    /**
     * This method is called to automatically
     * crop the image so that whitespace is
     * removed.
     *
     * @param w The width of the image.
     * @param h The height of the image
     */
    protected void findBounds(int w, int h) {
        // top line
        for (int y = 0; y < h; y++) {
            if (!hLineClear(y)) {
                downSampleTop = y;
                break;
            }

        }
        // bottom line
        for (int y = h - 1; y >= 0; y--) {
            if (!hLineClear(y)) {
                downSampleBottom = y;
                break;
            }
        }
        // left line
        for (int x = 0; x < w; x++) {
            if (!vLineClear(x)) {
                downSampleLeft = x;
                break;
            }
        }

        // right line
        for (int x = w - 1; x >= 0; x--) {
            if (!vLineClear(x)) {
                downSampleRight = x;
                break;
            }
        }
    }

    /**
     * Called to downsample a quadrant of the image.
     *
     * @param x The x coordinate of the resulting
     *          downsample.
     * @param y The y coordinate of the resulting
     *          downsample.
     * @return Returns true if there were ANY pixels
     * in the specified quadrant.
     */
    protected boolean downSampleQuadrant(int x, int y) {
        int w = width;
        int startX = (int) (downSampleLeft + (x * ratioX));
        int startY = (int) (downSampleTop + (y * ratioY));
        int endX = (int) (startX + ratioX);
        int endY = (int) (startY + ratioY);

        for (int yy = startY; yy <= endY; yy++) {
            for (int xx = startX; xx <= endX; xx++) {
                int loc = xx + (yy * w);

                if (pixelMap[loc] != 0)
                    return true;
            }
        }

        return false;
    }


    /**
     * Called to downsample the image and store
     * it in the down sample component.
     */
    public void downSample() {
        int w = width;
        int h = height;

        try {

            pixelMap = new int[w*h];
            entryGraphics.getPixels(pixelMap, 0, w, 0, 0, w, h);
            findBounds(w, h);

            SampleData data = sample.getData();

            ratioX = (double) (downSampleRight - downSampleLeft) / (double) data.getWidth();
            ratioY = (double) (downSampleBottom - downSampleTop) / (double) data.getHeight();

            for (int y = 0; y < data.getHeight(); y++) {
                for (int x = 0; x < data.getWidth(); x++) {
                    if (downSampleQuadrant(x, y))
                        data.setData(x, y, true);
                    else
                        data.setData(x, y, false);
                }
            }


        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(),"Errore Salvataggio:" + e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }
}