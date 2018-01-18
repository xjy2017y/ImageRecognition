package wj.redpacket.com.arredpacket;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.features2d.DescriptorMatcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/11.
 */

public class Match {
    public static final int TYPE = 1;
    public static FeaturePoint hidedPic;
    public static FeaturePoint findedPic;
    public static MatOfDMatch matches;
    public static List<DMatch> goodMatch;

    public static void setHidedPicKeyPoints(Bitmap pic){
        Mat hidedMat = new Mat();
        Utils.bitmapToMat(pic,hidedMat);
        hidedPic = new FeaturePoint(hidedMat,TYPE);
        hidedPic.setTargetKeyPoints();
    }
    public  static void setFindedPicKeyPoints(Bitmap pic){
        Mat findedMat = new Mat();
        Utils.bitmapToMat(pic,findedMat);
        findedPic = new FeaturePoint(findedMat,TYPE);
        findedPic.setTargetKeyPoints();
    }

    public static void getGoodMatch(){
        if (hidedPic == null || findedPic == null){
            Log.e("ERROR", "match:hidedPic or findedPic is null!!!");
            return;
        }else{
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            matches = new MatOfDMatch();
            hidedPic.good_Keypoints = new LinkedList<>();
            findedPic.good_Keypoints = new LinkedList<>();
            matcher.match(hidedPic.getDescriptors(),findedPic.getDescriptors(),matches);
            double max_dist = Double.MIN_VALUE;
            double min_dist = Double.MAX_VALUE;
            DMatch [] mats = matches.toArray();
            for(int i = 0;i < mats.length;i++){
                double dist = mats[i].distance;
                if (dist < min_dist) {
                    min_dist = dist;
                }
                if (dist > max_dist) {
                    max_dist = dist;
                }
            }
            goodMatch = new LinkedList<>();
            KeyPoint []  hidedKeyPoints = hidedPic.keyPoints.toArray();
            KeyPoint []  findedKeyPoints = findedPic.keyPoints.toArray();
            for (int i = 0; i < mats.length; i++) {
                double dist = mats[i].distance;
                if(dist < 0.6*max_dist){        //3*min_dist
                    KeyPoint tmp1;
                    tmp1 = hidedKeyPoints[mats[i].queryIdx];
                    hidedPic.good_Keypoints.add(tmp1);
                    KeyPoint tmp2;
                    tmp2 = findedKeyPoints[mats[i].trainIdx];
                    findedPic.good_Keypoints.add(tmp2);
                    goodMatch.add(mats[i]);
                }
            }
            hidedPic.good_feature_num = hidedPic.good_Keypoints.size();         //记录good_feature_num
            findedPic.good_feature_num = findedPic.good_Keypoints.size();
        }
    }

    public static void calculPic(FeaturePoint featurePoint){            //调取进行聚类运算
        featurePoint.getCluster();
        featurePoint.getPtDist();
        featurePoint.getDensity();
        featurePoint.getDist2HighDensity();
        featurePoint.clusterPoint(false);
    }

    public static boolean matchCluster(){           //相似度匹配
        Similarity similarity = new Similarity();
        similarity.matchCluster(hidedPic,findedPic);
        boolean ans = similarity.isSimilar(hidedPic,findedPic);
        return ans;
    }
}
