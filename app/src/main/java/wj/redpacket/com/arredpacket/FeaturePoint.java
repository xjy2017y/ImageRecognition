package wj.redpacket.com.arredpacket;

import android.graphics.Bitmap;
import android.media.audiofx.AudioEffect;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.*;
//import org.opencv.features2d.KeyPoint;
//import org.opencv.features2d.ORB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC4;

/**
 * Created by Administrator on 2018/1/11.
 */

public class FeaturePoint {
    private final int MAX_GROUP_NUM = 50;       //定义cr的大小
    private final double TOP = 0.1;         //原来是0.2
    private final int dc =30;      //截断距离
    private final int MAX_FEATURE_NUM = 1000;       //特征点最大数目
    public Mat rgbd;
    public MatOfKeyPoint keyPoints;
    public Mat descriptors;
    public List<KeyPoint> good_Keypoints;     //优秀特征点
//    public List<Destin>[] pt_dist;
    public double[][] pt_dist;
    public int cluster_num;         //聚类点
    public Cluster[] cr;
    public int good_feature_num;         //优秀特征点数目
//    private class Destin{
//        int des;        //终点目标
//        double distance;
//        Destin(int des,double distance){
//            this.des = des;
//            this.distance = distance;
//        }
//    }
    private class pointVal{         //内部类
        public int index;
        public double val;
        pointVal(int index,int val){
            this.index = index;
            this.val = val;
        }
    }
    private List<pointVal> rho;
    private List<pointVal> delta;
    private int type;

    FeaturePoint(Mat rgbd,int type){
        this.rgbd = rgbd;
        this.type = type;
    }

    public Mat getDescriptors() {
        return descriptors;
    }

    public Mat getRgbd() {
        return rgbd;
    }

    public void setRgbd(Mat rgbd) {
        this.rgbd = rgbd;
    }

    public MatOfKeyPoint getKeyPoints() {
        return keyPoints;
    }
    public void setTargetKeyPoints() {
        keyPoints = new MatOfKeyPoint();
        descriptors = new Mat();
        if (type == 1){         //使用ORB算法
            FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
            DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            detector.detect(rgbd, keyPoints);
            descriptor.compute(rgbd,keyPoints,descriptors);
//            Log.i("Image1",""+rgbd.rows()+"   "+rgbd.cols());
//            Mat result = new Mat(1080,1920,CV_8UC4);
//            Features2d.drawKeypoints(rgbd,keyPoints,result);
//            Bitmap bit  = null;
//            Utils.matToBitmap(result,bit);
//            FileOutputStream out;
//            File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis()+".jpg");
//            try {
//                out = new FileOutputStream(file);
//                bit.compress(Bitmap.CompressFormat.JPEG, 90, out);
//                System.out.println("___________保存的__sd___下_______________________");
//                out.flush();
//                out.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }else if(type == 2){
            FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
            DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
            detector.detect(rgbd,keyPoints);
            descriptor.compute(rgbd,keyPoints,descriptors);
        }
    }

    public void getCluster(){
//        rho = new pointVal[MAX_FEATURE_NUM];
//        delta = new pointVal[MAX_FEATURE_NUM];
        rho = new LinkedList<pointVal>();
        delta = new LinkedList<pointVal>();
        initCluster();
    }

    private void initCluster(){
//        pt_dist = new LinkedList[MAX_FEATURE_NUM];          //初始化pt_dist
//        for (int i = 0;i < MAX_FEATURE_NUM;i++){
//            pt_dist[i] = new LinkedList<Destin>();
//        }
        pt_dist = new double[MAX_FEATURE_NUM][MAX_FEATURE_NUM]; //初始化pt_dist
        for (int i = 0; i < good_feature_num; i++){         //初始化rho delta
            rho.add(new pointVal(i,0));
            delta.add(new pointVal(i,0));
        }
        cr = new Cluster[MAX_GROUP_NUM];                   //初始化cr
        for(int i = 0;i<MAX_GROUP_NUM;i++){
            cr[i] = new Cluster();
            cr[i].init();
        }
    }
    public void getPtDist(){            //计算任意两点直接的距离
        if (good_Keypoints.size() >MAX_FEATURE_NUM){
            Log.e("ERROR","good_Keypoints' size bigger than MAX_FEATURE_NUM");
            return;
        }
        for (int i = 0; i < good_feature_num;i++){
            for(int j = i;j < good_feature_num;j++){
                if(i == j){
                    pt_dist[i][j] = 0;
                }
                double dest = Math.pow(good_Keypoints.get(i).pt.x-good_Keypoints.get(j).pt.x,2) + Math.pow(good_Keypoints.get(i).pt.y-good_Keypoints.get(j).pt.y,2);
                dest = Math.pow(dest,0.5);
                pt_dist[i][j]=dest;

            }
        }
    }

    public void getDensity(){
        for(int i = 0;i < good_feature_num - 1;i++){
            for (int j = 0;j <good_feature_num;j++){
                if (getDistance(i,j)<=dc){
                    rho.get(i).val+=1;
                    rho.get(j).val+=1;
                }
            }
        }
    }

    public double getDistance(int x,int y){
        if (x <= y)
            return pt_dist[x][y];
        else
            return pt_dist[y][x];
    }
    public void getDist2HighDensity(){
        for(int i = 0;i<good_feature_num;i++){
            double dtmp = 0;
            boolean flag = false;
            for (int j = 0; j < good_feature_num; j++) {
                if (i == j)
                    continue;
                if (rho.get(j).val > rho.get(i).val) {				//如果存在j的票数大于i的票数的，则i的dtmp会记录与比他大票数的dis的最小值，作为dtmp。如果不存在j的票数大于i的票数的，i的dtmp取最大值。
                    double tmp = getDistance(i, j);
                    if (!flag) {
                        dtmp = tmp;		//记录距离
                        flag = true;
                    }else{
                        if (tmp < dtmp)
                            dtmp = tmp;		//记录最小距离？
                    }
                }
                if (!flag) {
                    for (int k = 0; k < good_feature_num; k++) {		//搜索距离最小的距离
                        double tmp = getDistance(i, k);
                        if (tmp > dtmp)
                            dtmp = tmp;
                    }
                }
//                delta[i].val = dtmp;		//记录
                delta.get(i).val = dtmp;
            }
        }
    }

    public void clusterPoint(boolean isNorm){
        if (isNorm == true){
            if (rho.size()!=delta.size()){
                Log.e("error","init rho and delta failed!!");
                return;
            }
            sort(rho);
            sort(delta);
            for (int i = 0;i < good_feature_num;i++){
                rho.get(i).val = (rho.get(i).val - rho.get(0).val) / (rho.get(good_feature_num - 1).val - rho.get(0).val);
                delta.get(i).val = (delta.get(i).val - delta.get(0).val) / (delta.get(good_feature_num - 1).val - delta.get(0).val);
            }
        }else {
            sort(rho);
            sort(delta);
        }
        rhoCluster();
    }

    public void sort(List<pointVal> str){           //升序排列
        for (int i = 0;i < str.size()-1;i++){
            for (int j = i+1;j<str.size();j++){
                if (str.get(j).val < str.get(i).val){
                    int index = str.get(j).index;
                    double val = str.get(j).val;
                    str.get(j).index = str.get(i).index;
                    str.get(j).val = str.get(i).val;
                    str.get(i).index = index;
                    str.get(i).val = val;
                }
            }
        }
    }

    public void rhoCluster(){
        int count = 0;      //聚类个数
        int pre_added = 0, cur_added = -1;		//pre_added为已经判断过的点的个数
        List<Boolean> isAdd = new ArrayList<Boolean>();
        for (int i = 0;i < good_feature_num;i++){
            isAdd.add(false);
        }
        for (int i = 0;i<good_feature_num*TOP;i++){
            if (!isAdd.get(i) && findDelta(rho.get(i).index,(int)(good_feature_num*(1 - TOP) - 1),good_feature_num)){   //如果票数少且dval也大，则是孤立点
                isAdd.set(rho.get(i).index,true);
                pre_added++;
            }
        }
        while(cur_added!=pre_added && !allTrue(isAdd)) {
            if (count > MAX_FEATURE_NUM){
                Log.e("error","count bigger than MAX_FEATURE_NUM");
                return;
            }
            cur_added = pre_added;
            for (int i = good_feature_num - 1; i >= 0; i--) {
                int rhoIndex = rho.get(i).index;
                if (!isAdd.get(rhoIndex)) {					//如果没有被分为孤立点
                    /***********************
                     if is there is no group
                     ************************/
                    if (count == 0) {					//新添聚类
                        count++;
                        cr[0].index = rhoIndex;			//聚类种子
                        cr[0].num = 1;
                        isAdd.set(rhoIndex,true);
                        pre_added++;
                    }
                    else {
                        int cluster_index = findNear(rhoIndex, count);		//得到距离最近的聚类种子点的index

                        if (cluster_index >= 0) {			//找到了又距离近的点
                            /***********************
                             if it can be included in
                             a previous cluster
                             ************************/
                            isAdd.set(rhoIndex,true);
                            pre_added++;
                            cr[cluster_index].num++;												//归为聚类
                            cr[cluster_index].radius += getDistance(cr[cluster_index].index, rhoIndex);		//聚类半径扩大
                            cr[cluster_index].other.add(i);
                        }
                        else {
                            /***********************
                             if rho and delta are both
                             more than other points
                             没找到
                             ************************/
                            if (i >= (int)(good_feature_num*(1 - TOP*1.5) - 1) && findDelta(rhoIndex, (int)(good_feature_num*(1 - TOP*1.5) - 1), good_feature_num)) {			//找到票数多且半径小的点，不是孤立点
                                isAdd.set(rhoIndex,true);
                                pre_added++;
                                count++;					//新增聚类点
                                cr[count - 1].index =rhoIndex;		//记录聚类中心
                                cr[count - 1].num = 1;					//记录聚类点附近点数量
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            if (cr[i].num > 1)			//如果聚类点数量多于1
                cr[i].radius = 1.0 * cr[i].radius / (cr[i].num - 1);			//求平均radius
            else if (cr[i].num == 1) {				//如果只有一个聚类点，则去除这个点
                for (int j = i + 1; j < count; j++) {
                    cr[j - 1] = cr[j];
                }
                count--;
            }
        }

        cluster_num = count;
    }

    public int findNear(int ri,int cur_num){
        int cluster_index = -1;
        double min_dist = 0;
        boolean flag = false;
        for (int i = 0; i < cur_num; i++) {
            double tmp = getDistance(cr[i].index, ri);			//获取聚类种子点到ri的距离
            if (!flag) {		//如果首次判断
                min_dist = tmp;
                flag = true;
                if (tmp <= dc*1.3) {			//如果小于，记录当前的聚类种子点   为什么不直接用dc？
                    cluster_index = i;
                }
            }
            else {
                if (tmp < min_dist && tmp <= dc) {			//如果存在比当前距离更小的，则记录这个聚类种子点
                    min_dist = tmp;
                    cluster_index = i;
                }
            }
        }
        return cluster_index;
    }

    public boolean findDelta(int ri,int from,int to){
        for (int j = from;j < to;j++){
            if (delta.get(j).index == ri)
                return true;
        }
        return false;
    }

    public boolean allTrue(List<Boolean> isAdd){
        for (int i = 0;i < isAdd.size();i++){
            if (isAdd.get(i) == false){
                return false;
            }
        }
        return true;
    }
}
