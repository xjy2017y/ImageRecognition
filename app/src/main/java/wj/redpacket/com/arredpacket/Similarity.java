package wj.redpacket.com.arredpacket;

import android.util.Log;

import org.opencv.core.Mat;

import java.util.List;

/**
 * Created by Administrator on 2018/1/15.
 */

public class Similarity {
    private final int MAX_GROUP_NUM = 50;
    private final double NUM_RATIO = 0.6;
    private final double CLR_RATIO = 0.4;
    private final double DIST_RATIO = 0.2;
    public int match_cout;
    public int vaild_match;
    public int point_cout;
    public int vaild_point;
    public matchList[] ml;
    private class matchList {
        public int c1_index;
        public int r1;
        public int g1;
        public int b1;
        public int c2_index;
        public int r2;
        public int g2;
        public int b2;
        public boolean isVaild;
    }
    public void initalSimilarity(){
        match_cout = 0;
        vaild_match = 0;
        point_cout = 0;
        vaild_point = 0;
        ml = new matchList[MAX_GROUP_NUM];
        for (int i = 0; i < MAX_GROUP_NUM; i++) {			//初始化rgb值
            ml[i] = new matchList();
            ml[i].c1_index = -1;
            ml[i].r1 = 0;
            ml[i].g1 = 0;
            ml[i].b1 = 0;
            ml[i].c2_index = -1;
            ml[i].r2 = 0;
            ml[i].g2 = 0;
            ml[i].b2 = 0;
            ml[i].isVaild = false;
        }
    }
    public void matchCluster(FeaturePoint fa1,FeaturePoint fa2){
        initalSimilarity();
        int pre_match = 0, cur_match = -1;
        while (cur_match != pre_match && !isAllMatch(fa1,fa2) ) {
            cur_match = pre_match;
            for (int i = 0; i < fa1.cluster_num; i++) {            //遍历fa1聚类点
                if (!fa1.cr[i].isMatched){
                    for (int j = 0; j < fa2.cluster_num; j++) {
                        if (!fa1.cr[i].isMatched && !fa2.cr[j].isMatched) {
                            /****************************************
                             if NUM_RATIO*point number in both clusters
                             belongs to other, than they are match
                             BUT HERE, i consider if one side suits
                             the demand, than it matches BECAUSE:
                             finding fa2 matches fa1 is difficultic
                             ****************************************/
                            int q2t = 0;
                            if (isInclude(fa1.cr[i].index, fa2.cr[j])) {		//判断fa1的index是否包含在fa2的聚类点或者other中？
                                q2t++;
                            }
                            for (int k = 0; k < fa1.cr[i].other.size(); k++) {		//判断fa1的other是否包含在fa2的聚类点或者other中
                                if (isInclude(fa1.cr[i].other.get(k), fa2.cr[j])) {			//如果在，则q2t++
                                    q2t++;
                                }
                            }
                            if (q2t >= (int)NUM_RATIO*fa1.cr[i].num-1) {		//如果匹配数超过0.6*聚类个数，就算匹配上了
                                pre_match++;
                                ml[match_cout].c1_index = i;					//match_cout初始化为0了
                                ml[match_cout].c2_index = j;
                                fa1.cr[i].isMatched = true;
                                fa2.cr[j].isMatched = true;
                                match_cout++;
                                //cout << "!!!success: i= " << i << "  j= " << j << endl;
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isAllMatch(FeaturePoint fa1,FeaturePoint fa2){
        boolean flag1 = true;
        boolean flag2 = true;
        for (int i = 0; i < fa1.cluster_num; i++) {
            if (fa1.cr[i].isMatched == false) {
                flag1 = false;
                break;
            }
        }
        for (int i = 0; i < fa2.cluster_num; i++) {
            if (fa2.cr[i].isMatched == false)
            {
                flag2 = false;
                break;
            }
        }
        if (flag1 == false && flag2 == false)
            return false;
        return true;
    }

    public boolean isInclude(int idx,Cluster cr){
        if (cr.index == idx)
            return true;
        for (int i = 0; i < cr.other.size(); i++) {
            if (cr.other.get(i) == idx)
                return true;
        }
        return false;
    }

    public boolean isSimilar(FeaturePoint fa1,FeaturePoint fa2){
        vaild_match = match_cout;
        Log.i("Image1 match_cout is",""+match_cout);
        for (int k = 0; k < match_cout; k++) {
            getClrVal(k, fa1, fa2);			//对一对similar聚类内的点RGB3色取平均值
            if (clrVaild(ml[k].r1, ml[k].g1, ml[k].b1, ml[k].r2, ml[k].g2, ml[k].b2) && posValid(ml[k].c1_index, ml[k].c2_index,fa1,fa2)){	//对平均值进行对比，3色中有俩色符合即为true    //&& posValild(ml[k].c1_index, ml[k].c2_index, fp.rgbd1.cols, fp, fa1, fa2)
                ml[k].isVaild = true;
                vaild_point += fa2.cr[k].num;
            }
            else
                vaild_match--;			//不符合则match数减少
        }
        if (match_cout == 0)
            return false;
        Log.i("Image1 vaild_match is",""+vaild_match);
        if (vaild_match < match_cout*0.80)			//match数少于一定数目返回false
            return false;

        return true;
    }

    public boolean posValid(int idx1,int idx2,FeaturePoint fa1,FeaturePoint fa2){
        double x1 = fa1.good_Keypoints.get(fa1.cr[idx1].index).pt.x;
        double y1 = fa1.good_Keypoints.get(fa1.cr[idx1].index).pt.y;
        double x2 = fa2.good_Keypoints.get(fa2.cr[idx2].index).pt.x;
        double y2 = fa2.good_Keypoints.get(fa2.cr[idx2].index).pt.y;
        double distx = Math.abs(x1 - x2);
        double disty = Math.abs(y1 - y2);
        if (distx > fa1.rgbd.cols()*DIST_RATIO || disty > fa1.rgbd.cols()*DIST_RATIO)
            return false;

        return true;
    }

    public void getClrVal(int idx,FeaturePoint fa1,FeaturePoint fa2){
        Mat p1 = fa1.rgbd;
        Mat p2 = fa2.rgbd;
        //Log.i("Image1 p1值的大小",""+p1.rows()+ "   "+p1.cols());
        //Log.i("Image1 other.size is",""+fa1.cr[ml[idx].c1_index].other.size());
        for (int i = 0; i < fa1.cr[ml[idx].c1_index].other.size(); i++) {			//对cr的聚类的other点进行遍历
            int x, y;
            //x = (int)fp.Keypoints1[i].pt.x;				//这里为啥不是Keypoints1[other[i]]?
            //y = (int)fp.Keypoints1[i].pt.y;
            int num = fa1.cr[ml[idx].c1_index].other.get(i);
            y = (int)fa1.good_Keypoints.get(num).pt.x;
            x = (int)fa1.good_Keypoints.get(num).pt.y;
            //Log.i("Image1 x , y  i的次数",""+x+"   "+y+"    "+i);
            //Log.i("Image1 p1.get",""+p1.get(x,y)[0]);
            ml[idx].r1 +=p1.get(x,y)[0];
            ml[idx].g1 +=p1.get(x,y)[1];
            ml[idx].b1 +=p1.get(x,y)[2];
        }
        int cluster = fa1.cr[ml[idx].c1_index].index;
        int y1 = (int)fa1.good_Keypoints.get(cluster).pt.x;
        int x1 = (int)fa1.good_Keypoints.get(cluster).pt.y;
        ml[idx].r1 = (int)((ml[idx].r1 + p1.get(x1,y1)[0])/fa1.cr[ml[idx].c1_index].num);
        ml[idx].g1 = (int)((ml[idx].g1 + p1.get(x1,y1)[1])/fa1.cr[ml[idx].c1_index].num);
        ml[idx].b1 = (int)((ml[idx].b1 + p1.get(x1,y1)[2])/fa1.cr[ml[idx].c1_index].num);

        //fa2
        for (int i = 0; i < fa2.cr[ml[idx].c2_index].other.size(); i++) {			//对cr的聚类的other点进行遍历
            int x, y;
            //x = (int)fp.Keypoints1[i].pt.x;				//这里为啥不是Keypoints1[other[i]]?
            //y = (int)fp.Keypoints1[i].pt.y;
            int num = fa2.cr[ml[idx].c2_index].other.get(i);
            y = (int)fa2.good_Keypoints.get(num).pt.x;
            x = (int)fa2.good_Keypoints.get(num).pt.y;
            ml[idx].r2 +=p2.get(x,y)[0];
            ml[idx].g2 +=p2.get(x,y)[1];
            ml[idx].b2 +=p2.get(x,y)[2];
        }
        int cluster2 = fa2.cr[ml[idx].c2_index].index;
        int y = (int)fa2.good_Keypoints.get(cluster2).pt.x;
        int x = (int)fa2.good_Keypoints.get(cluster2).pt.y;
        ml[idx].r2 = (int)((ml[idx].r2 + p2.get(x,y)[0])/fa2.cr[ml[idx].c2_index].num);
        ml[idx].g2 = (int)((ml[idx].g2 + p2.get(x,y)[1])/fa2.cr[ml[idx].c2_index].num);
        ml[idx].b2 = (int)((ml[idx].b2 + p2.get(x,y)[2])/fa2.cr[ml[idx].c2_index].num);
    }

    public boolean clrVaild(int a,int b,int c,int x,int y,int z){
        int count = 0;
        if (Math.abs(a - x) / (double)a <= CLR_RATIO)
        count++;
        if (Math.abs(b - y) / (double)b <= CLR_RATIO)
        count++;
        if (Math.abs(c - z) / (double)c <= CLR_RATIO)
        count++;

        if (count >= 2)
            return true;

        return false;
    }
}
