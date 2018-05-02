package pers.philips.entity;

public class InstanceModel {

    private int modelId;        //模组id
    private int cpuExpect;      //预期cpu使用量
    private int memExpect;      //预期内存使用量
    private int hdExpect;       //预期硬盘使用量
    private int nwExpect;       //预期网络带宽使用量


    /**
     * InstanceModel Constructor
     * @param mid
     * @param cpu
     * @param mem
     * @param hd
     * @param nw
     */
    public InstanceModel(int mid, int cpu, int mem, int hd, int nw)
    {
        this.modelId = mid;
        this.cpuExpect = cpu;
        this.memExpect = mem;
        this.hdExpect = hd;
        this.nwExpect = nw;
    }

    /**
     * getter
     * @return
     */
    public int getModelId() {
        return modelId;
    }
    public int getCpuExpect() {
        return cpuExpect;
    }
    public int getMemExpect() {
        return memExpect;
    }
    public int getHdExpect() {
        return hdExpect;
    }
    public int getNwExpect() {
        return nwExpect;
    }
}
