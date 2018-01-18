package wj.redpacket.com.arredpacket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
//        Bitmap mBitmap = ((BitmapDrawable) appContext.getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();
//        Match.setHidedPicKeyPoints(mBitmap);
//        Match.setFindedPicKeyPoints(mBitmap);
//        Match.getGoodMatch();
//        Match.calculPic(Match.findedPic);
//        Match.calculPic(Match.hidedPic);
//        boolean ans = Match.matchCluster();
//        Log.i("answer is",""+ans);
        //assertEquals("wj.redpacket.com.arredpacket", appContext.getPackageName());
    }
}
