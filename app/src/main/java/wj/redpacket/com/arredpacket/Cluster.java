package wj.redpacket.com.arredpacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/12.
 */

public class Cluster {
    public int index;
    public boolean isMatched;
    public double radius;
    public int num; //种子数量
    public List<Integer> other;
    public void init(){
        isMatched = false;
        radius = 0;
        other = new ArrayList<>();
    }
}
