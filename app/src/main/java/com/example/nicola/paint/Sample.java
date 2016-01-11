package com.example.nicola.paint;

import android.graphics.Bitmap;


/**
 * This component will display a downsampled image.
 * Downsampleing is the process of taking a hi-res
 * image to a much lower resolution.
 */

public class Sample {

    /**
     * The image data.
     */
    SampleData data;

    /**
     * The constructor.
     *
     * @param width The width of the downsampled image
     * @param height The height of the downsampled image
     */
    Sample(int width, int height)
    {
        data = new SampleData(' ', width, height);
    }

    /**
     * The image data object.
     *
     * @return The image data object.
     */
    SampleData getData()
    {
        return data;
    }

    /**
     * Assign a new image data object.
     *
     * @param data The image data object.
     */

    void setData(SampleData data)
    {
        this.data = data;
    }

    /**
     * Return the downsample image
     * @param cell_dimension the dimension of the single square
     * @return the downsaple image
     */
    public Bitmap paint(int cell_dimension)
    {
        if ( data == null )
            return null;

        int height = data.getHeight() * cell_dimension;
        int width = data.getWidth() * cell_dimension;

        // this creates a MUTABLE bitmap
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for(int i = 0; i < data.getWidth(); i++)
            for(int j = 0; j < data.getHeight(); j++)
                if(data.getData(i, j))
                    colorArea(bmp, i * cell_dimension , j * cell_dimension, cell_dimension, 0xFF000000);
                else
                    colorBorder(bmp, i * cell_dimension, j * cell_dimension, cell_dimension, 0xFF000000);

        return bmp;
    }

    private static void colorArea(Bitmap bmp, int x, int y, int dim, int color){
        for(int i = x; i < x + dim; i++)
            for(int j = y; j < y + dim; j++)
                bmp.setPixel(i, j, color);
    }

    private static void colorBorder(Bitmap bmp, int x, int y, int dim, int color){
        for(int i = x; i < x + dim; i++)
            for(int j = y; j < y + dim; j++)
                if(i==x || i==x+dim-1 || j == y || j==y+dim-1)
                    bmp.setPixel(i, j, color);
    }
}