package pers.philips.entity;

import java.util.ArrayList;
import java.util.Iterator;

public class Server {
    private int serverId;   //服务器编号
    private int cpuTotal;   //cpu总数
    private int memTotal;   //内存总量
    private int hdTotal;    //硬盘总量
    private int nwTotal;    //网络带宽总量

    private double cpuUsage;
    private double memUsage;
    private double hdUsage;
    private double nwUsage;
    private ArrayList<InstanceModel> instances;   //已部署在该server的instantce数组

    /**
     * Server Constructor
     * @param sid
     * @param cpu
     * @param mem
     * @param hd
     * @param nw
     */
    public Server(int sid, int cpu, int mem, int hd, int nw)
    {
        this.serverId = sid;
        this.cpuTotal = cpu;
        this.memTotal = mem;
        this.hdTotal = hd;
        this.nwTotal = nw;
        this.instances = new ArrayList<InstanceModel>();
    }

    /**
     * Getters
     */
    public int getServerId() {
        return serverId;
    }
    public int getCpuTotal() {
        return cpuTotal;
    }
    public int getMemTotal() {
        return memTotal;
    }
    public int getHdTotal() {
        return hdTotal;
    }
    public int getNwTotal() {
        return nwTotal;
    }
    public double getCpuUsage() {
        return cpuUsage;
    }
    public double getMemUsage() {
        return memUsage;
    }
    public double getHdUsage() {
        return hdUsage;
    }
    public double getNwUsage() {
        return nwUsage;
    }

    /**
     * Setters
     */
    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }
    public void setMemUsage(double memUsage) {
        this.memUsage = memUsage;
    }
    public void setHdUsage(double hdUsage) {
        this.hdUsage = hdUsage;
    }
    public void setNwUsage(double nwUsage) {
        this.nwUsage = nwUsage;
    }

    /**
     * 部署instance
     * @param ins
     * @return
     */
    public boolean attachInstance(InstanceModel ins)
    {
        this.instances.add(ins);
        return true;
    }

    /**
     * 根据模组id关闭instance
     * @param mid
     * @return
     */
    public boolean detachInstanceByModelId(int mid)
    {
        Iterator<InstanceModel> iIns = this.instances.iterator();
        while(iIns.hasNext())
        {
            if(iIns.next().getModelId() == mid)
            {
                iIns.remove();
                break;
            }
        }
        return true;
    }

    /**
     * 获取物理机上部署的某id instance的个数
     * @param mid
     * @return
     */
    public int countInstanceByModelId(int mid)
    {
        int result = 0;
        Iterator<InstanceModel> iIns = this.instances.iterator();
        while(iIns.hasNext())
        {
            if(iIns.next().getModelId() == mid)
            {
                iIns.remove();
                result++;
            }
        }
        return result;
    }


    /**
     * 获取物理机可用CPU个数
     * @return
     */
    public int getCpuAvail()
    {
        return (int)((double)(this.cpuTotal)*(1D - this.cpuUsage));
    }

    /**
     * 获取物理机可用内存
     * @return
     */
    public int getMemAvail()
    {
        return (int)((double)(this.memTotal)*(1D - this.memUsage));
    }

    /**
     * 获取物理机可用硬盘
     * @return
     */
    public int getHdAvail()
    {
        return (int)((double)(this.hdTotal)*(1D - this.hdUsage));
    }

    /**
     * 获取物理机可用网络带宽
     * @return
     */
    public int getNwAvail()
    {
        return (int)((double)(this.nwTotal)*(1D - this.nwUsage));
    }

    /**
     * 物理机的剩余资源是否足够部署instance模组
     * @param ins
     * @return
     */
    public boolean attachable(InstanceModel ins)
    {
        return  (this.getCpuAvail() >= ins.getCpuExpect()) &&
                (this.getMemAvail() >= ins.getMemExpect()) &&
                (this.getHdAvail() >= ins.getHdExpect()) &&
                (this.getNwAvail() >= ins.getNwExpect());
    }

    /**
     * 计算部署模组后的CPU使用率
     * @param ins
     * @return
     */
    public double getCpuUsageAfterDeploy(InstanceModel ins)
    {
        return 1D - ((double)(getCpuAvail() - ins.getCpuExpect()) / (double)cpuTotal);
    }

    /**
     * 计算部署模组后的内存使用率
     * @param ins
     * @return
     */
    public double getMemUsageAfterDeploy(InstanceModel ins)
    {
        return 1D - ((double)(getMemAvail() - ins.getMemExpect()) / (double)memTotal);
    }

    /**
     * 计算部署模组后的硬盘使用率
     * @param ins
     * @return
     */
    public double getHdUsageAfterDeploy(InstanceModel ins)
    {
        return 1D - ((double)(getHdAvail() - ins.getHdExpect()) / (double)hdTotal);
    }

    /**
     * 计算部署模组后的网络使用率
     * @param ins
     * @return
     */
    public double getNwUsageAfterDeploy(InstanceModel ins)
    {
        return 1D - ((double)(getNwAvail() - ins.getNwExpect()) / (double)nwTotal);
    }

}
