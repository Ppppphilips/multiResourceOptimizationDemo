package pers.philips.util;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import java.util.ArrayList;
import java.util.Iterator;


public class MathUtility {

    /**
     * 根据输入数列计算并返回标准差
     * @param numberLists
     * @return
     */
    public static final double getStandardDeviation(ArrayList<Double> numberLists)
    {
        SummaryStatistics data = new SummaryStatistics();
        Iterator<Double> iNumber = numberLists.iterator();
        while(iNumber.hasNext())
        {
            data.addValue(iNumber.next());
        }
        return Math.sqrt(data.getPopulationVariance());
    }
}
