package wj.redpacket.com.arredpacket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.math.*;
import java.text.DecimalFormat;

/**
 * Created by jiangwei on 17/4/4.
 */

public class BitmapCompare {
    private static final int TYPE = 1;
    private static final String DIFFERENT_SIZE = "differentSize";
    private static final String RESULT_FORMAT = "00.0%";
    private static final int SHIFT = 40;

    public static float similarity(String url1, String url2) {
        Bitmap bm1 = BitmapFactory.decodeFile(url1);
        Bitmap bm2 = BitmapFactory.decodeFile(url2);
        return similarity(bm1, bm2);
    }

    public static float similarity(Bitmap bm1, Bitmap bm2) {
        final int bm1Width = bm1.getWidth();
        final int bm2Width = bm2.getWidth();
        final int bmHeight = bm1.getHeight();

        if (bmHeight != bm2.getHeight() || bm1Width != bm2Width)
            return 0;

        int[] pixels1 = new int[bm1Width];
        int[] pixels2 = new int[bm2Width];

        reset();
        for (int i = 0; i < bmHeight; i++) {
            bm1.getPixels(pixels1, 0, bm1Width, 0, i, bm1Width, 1);
            bm2.getPixels(pixels2, 0, bm2Width, 0, i, bm2Width, 1);
            comparePixels(pixels1, pixels2, bm1Width);
        }

        float result =  Count.sT * 1.0f / (Count.sF + Count.sT);
//        return percent(Count.sT, Count.sF + Count.sT);
        return result;
    }

    private static void comparePixels(int[] pixels1, int[] pixels2, int length) {
        for (int i = 0; i < length; i++) {
            if (clrVaild(Color.red(pixels1[i]),Color.green(pixels1[i]),Color.blue(pixels1[i]),Color.red(pixels2[i]),Color.green(pixels2[i]),Color.blue(pixels2[i]))) {
                Count.sT++;
            } else {
                Count.sF++;
            }
        }
    }

    private static boolean clrVaild(int r1,int g1,int b1,int r2,int g2,int b2){
        int count = 0;
        if(Math.abs(r1-r2)<SHIFT){
            count++;
        }
        if(Math.abs(g1-g2)<SHIFT){
            count++;
        }
        if(Math.abs(b1-b2)<SHIFT){
            count++;
        }
        if (count >= 2){
            return true;
        }else {
            return false;
        }
    }
    private static String percent(int divisor, int dividend) {
        final double value = divisor * 1.0 / dividend;
        DecimalFormat df = new DecimalFormat(RESULT_FORMAT);
        return df.format(value);
    }

    private static void reset() {
        Count.sT = 0;
        Count.sF = 0;
    }

    private static class Count {
        private static int sT;
        private static int sF;
    }
}
