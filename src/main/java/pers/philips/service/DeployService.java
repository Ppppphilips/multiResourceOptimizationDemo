package pers.philips.service;

import com.alibaba.fastjson.JSONObject;
import pers.philips.entity.InstanceModel;
import pers.philips.entity.Server;
import pers.philips.util.Constants;
import pers.philips.util.MathUtility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class DeployService {
    private ArrayList<InstanceModel> models = new ArrayList<InstanceModel>();  //所有的模组信息
    private ArrayList<Server> servers = new ArrayList<Server>();        //所有现有的物理机
    private ArrayList<Server> serversForAttach = new ArrayList<Server>();   //可供扩容的物理机
    private ArrayList<Server> serversForDetach = new ArrayList<Server>();    //可供缩容的物理机
    private static final boolean ATTACH_FLG= true;
    private static final boolean DETACH_FLG= false;

    public DeployService()
    {
        // 初始化所有模组类型
        if(! initInstanceModels())
        {
            System.out.println("initInstanceModels failed");
        }

        // 初始化所有物理机信息以及已部署的instance
        if(! initServersInstances())
        {
            System.out.println("initServersInstances failed");
        }

        this.serversForAttach = new ArrayList<Server>(this.servers);
        this.serversForDetach = new ArrayList<Server>(this.servers);
    }

    /**
     * 初始化所有模组类型
     * @return
     */
    private boolean initInstanceModels()
    {
        try {
            BufferedReader modelReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Constants.MODEL_FILE)),
                    "UTF-8"));
            String lineJson = null;
            while ((lineJson = modelReader.readLine()) != null) {
                // 注释行过滤
                if(! lineJson.matches("^#.*"))
                {
                    JSONObject contents = JSONObject.parseObject(lineJson);
                    InstanceModel model = new InstanceModel(contents.getInteger("id"),contents.getInteger("cpu"),contents.getInteger("mem"),contents.getInteger("hd"),contents.getInteger("network"));
                    this.models.add(model);
                }
            }
            modelReader.close();
            return true;
        } catch (Exception e) {
            System.err.println("read errors :" + e);
            return false;
        }
    }

    /**
     * 初始化所有物理机信息以及已部署的instance
     * @return
     */
    private boolean initServersInstances()
    {
        try {
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Constants.SERVER_FILE)),
                    "UTF-8"));
            String lineJson = null;
            while ((lineJson = serverReader.readLine()) != null) {
                //过滤注释行
                if(! lineJson.matches("^#.*"))
                {
                    JSONObject contents = JSONObject.parseObject(lineJson);
                    Server server = new Server(contents.getInteger("id"),Integer.parseInt(Constants.getProperty("serverCPU")),Integer.parseInt(Constants.getProperty("serverMem")),Integer.parseInt(Constants.getProperty("serverHD")),Integer.parseInt(Constants.getProperty("serverNetwork")));
                    server.setCpuUsage(contents.getDoubleValue("cpuUsage"));
                    server.setMemUsage(contents.getDoubleValue("memUsage"));
                    server.setHdUsage(contents.getDoubleValue("hdUsage"));
                    server.setNwUsage(contents.getDoubleValue("nwUsage"));
                    this.servers.add(server);
                    JSONObject instances = contents.getJSONObject("instances");
                    int i;
                    for (Map.Entry<String, Object> entry : instances.entrySet()) {
                        for(i = 0; i < Integer.parseInt(entry.getValue().toString()); i++)
                        {
                            InstanceModel model = createInstanceByModelId(Integer.parseInt(entry.getKey()));
                            server.attachInstance(model);
                        }
                    }
                }
            }
            serverReader.close();
            return true;
        } catch (Exception e) {
            System.err.println("read errors :" + e);
            return false;
        }
    }

    /**
     * 根据instance模组id创建对象
     * @param mid
     * @return
     */
    private InstanceModel createInstanceByModelId(int mid)
    {
        Iterator<InstanceModel> iModel = this.models.iterator();
        while(iModel.hasNext())
        {
            InstanceModel model = iModel.next();
            if(mid == model.getModelId())
            {
                return new InstanceModel(model.getModelId(),model.getCpuExpect(),model.getMemExpect(),model.getHdExpect(),model.getNwExpect());
            }
        }
        return null;
    }

    /**
     * 获取扩容/缩容的模组对象
     * @param attachFlg 值为true时获取缩容模组id,false则获取缩容的模组id
     * @return
     */
    private int getInstanceForDeploy(boolean attachFlg)
    {
        int instanceId = 0;
        String fileName = null;
        if(attachFlg == ATTACH_FLG)
        {
            fileName = Constants.ATTACH_FILE;
        }
        else
        {
            fileName = Constants.DETACH_FILE;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName)),
                            "UTF-8"));
            String lineTxt = null;
            while ((lineTxt = reader.readLine()) != null) {
                // 注释行过滤
                if (! lineTxt.matches("^#.*"))
                {
                    instanceId = Integer.parseInt(lineTxt);
                    break;
                }
            }
            reader.close();
            return instanceId;
        } catch (Exception e) {
            System.err.println("read errors :" + e);
            return instanceId;
        }
    }

    /**
     * 计算扩容方案
     * @return
     */
    public void getAttachSolution()
    {
        int instanceId = getInstanceForDeploy(ATTACH_FLG);
        InstanceModel insToAttach = createInstanceByModelId(instanceId);
        printInputInfo(insToAttach,ATTACH_FLG);
        // 计算剩余资源可扩容的物理机
        calAvailServersForAttach(insToAttach);
        // 计算符合高可用条件可扩容的物理机
        calAvailServersUnderHAForAttach(instanceId);
        if(this.serversForAttach.size() == 0)
        {
            System.out.println("需要新增物理机后再部署模组" + instanceId );
            return;
        }
        // 计算标准差的改善值
        ArrayList<Double> dvsDiff = calDeviationDiff(calCurrentDeviation(),calDeployedDeviation(insToAttach));
        int bestIdx = getBestIndex(dvsDiff);
        int serverId = this.serversForAttach.get(bestIdx).getServerId();
        System.out.println("模组" + instanceId + "应部署在服务器" + serverId + "上" );
        //printDetailInformation();

    }

    /**
     * 计算剩余资源可扩容的物理机
     * @param ins
     * @return
     */
    private boolean calAvailServersForAttach(InstanceModel ins)
    {
        Iterator<Server> iServer = this.serversForAttach.iterator();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            if(! server.attachable(ins))
            {
                this.serversForAttach.remove(server);
            }
        }
        return true;
    }

    /**
     * 计算符合高可用条件可扩容的物理机
     * @param mid
     * @return
     */
    private boolean calAvailServersUnderHAForAttach(int mid)
    {
        int deployedNum = 0;
        Iterator<Server> iServer= this.serversForAttach.iterator();
        ArrayList<Server> undeployedServers = new ArrayList<Server>();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            if(server.countInstanceByModelId(mid) > 0)
            {
                deployedNum++;
            }
            else
            {
                undeployedServers.add(server);
            }
        }
        if(deployedNum < Integer.parseInt(Constants.getProperty("haNum")))
        {
            this.serversForAttach = new ArrayList<Server>(undeployedServers);
        }
        return true;
    }


    /**
     * 获取当前各类资源使用的标准差
     * @return
     */
    private ArrayList<Double> calCurrentDeviation()
    {
        ArrayList<Double> curDvs = new ArrayList<Double>();
        Iterator<Server> iServer= this.serversForAttach.iterator();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            ArrayList<Double> params = new ArrayList<Double>();
            params.add(server.getCpuUsage());
            params.add(server.getMemUsage());
            params.add(server.getHdUsage());
            params.add(server.getNwUsage());
            double dvs = MathUtility.getStandardDeviation(params);
            curDvs.add(dvs);
        }
        return curDvs;
    }

    /**
     * 获取当前各类资源使用的标准差
     * @return
     */
    private ArrayList<Double> calDeployedDeviation(InstanceModel ins)
    {
        ArrayList<Double> afterDvs = new ArrayList<Double>();
        Iterator<Server> iServer= this.serversForAttach.iterator();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            ArrayList<Double> params = new ArrayList<Double>();
            params.add(server.getCpuUsageAfterDeploy(ins));
            params.add(server.getMemUsageAfterDeploy(ins));
            params.add(server.getHdUsageAfterDeploy(ins));
            params.add(server.getNwUsageAfterDeploy(ins));
            double dvs = MathUtility.getStandardDeviation(params);
            afterDvs.add(dvs);
        }
        return afterDvs;
    }

    /**
     * 获取当前各类资源使用的标准差
     * @return
     */
    private ArrayList<Double> calDeviationDiff(ArrayList<Double> curDvs,ArrayList<Double> afterDvs)
    {
        ArrayList<Double> dvsDiff = new ArrayList<Double>();
        int i;
        for(i = 0; i < curDvs.size(); i++)
        {
            dvsDiff.add( curDvs.get(i) - afterDvs.get(i));
        }
        return dvsDiff;
    }

    /**
     * 获取最优index
     * @param dvsDiff
     * @return
     */
    private int getBestIndex(ArrayList<Double> dvsDiff)
    {
        int i,idx = 0;
        double best = -1D;
        for(i = 0; i < dvsDiff.size(); i ++)
        {
            if(dvsDiff.get(i) > best)
            {
                best = dvsDiff.get(i);
                idx = i;
            }
        }
        return idx;
    }


    /**
     * 计算缩容方案
     * @return
     */
    public void getDetachSolution()
    {
        int instanceId = getInstanceForDeploy(DETACH_FLG);
        InstanceModel insToDetach = createInstanceByModelId(instanceId);
        printInputInfo(insToDetach,DETACH_FLG);
        // 计算剩余资源可缩容的物理机
        calAvailServersForDetach(instanceId);
        // 计算符合高可用条件可缩容的物理机
        calAvailServersUnderHAForDetach(instanceId);
        if(this.serversForDetach.size() == 0)
        {
            System.out.println("很抱歉，没有符合条件的缩容方案");
            return;
        }
        // 计算最优缩容方案
        int leastNum = 99999;
        int serverId= -1;

        Iterator<Server> iServer = this.serversForDetach.iterator();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            if(server.getDeployedInstanceNum() < leastNum)
            {
                leastNum = server.getDeployedInstanceNum();
                serverId = server.getServerId();
            }
        }
        System.out.println("模组" + instanceId + "应从服务器" + serverId + "上缩容" );


    }

    /**
     * 获取可缩容的服务器
     * @param mid
     */
    private void calAvailServersForDetach(int mid)
    {
        Iterator<Server> iServer= this.serversForDetach.iterator();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            if(server.countInstanceByModelId(mid) == 0)
            {
                iServer.remove();
            }
        }
    }

    /**
     * 获取可缩容并且满足高可用条件的服务器
     * @param mid
     */
    private void calAvailServersUnderHAForDetach(int mid)
    {
        int deployedNum = 0;
        Iterator<Server> iServer= this.serversForDetach.iterator();
        ArrayList<Server> multiDeployedServers = new ArrayList<Server>();
        while(iServer.hasNext())
        {
            Server server = iServer.next();
            if(server.countInstanceByModelId(mid) > 1)
            {
                multiDeployedServers.add(server);
                deployedNum++;
            }
            else if(server.countInstanceByModelId(mid) == 1)
            {
                deployedNum++;
            }
            else
            {
                iServer.remove();
            }
        }
        if(deployedNum == Integer.parseInt(Constants.getProperty("haNum")))
        {
            this.serversForDetach = new ArrayList<Server>(multiDeployedServers);
        }
        if(deployedNum < Integer.parseInt(Constants.getProperty("haNum")))
        {
            System.out.println("当前环境不符合高可用条件");
            this.serversForDetach = new ArrayList<Server>();
        }
    }

    private void printInputInfo(InstanceModel ins, boolean attachFlg) {
        if (attachFlg == ATTACH_FLG)
        {
            System.out.println("扩容模组id:" + ins.getModelId());
            System.out.println("预期使用CPU" + ins.getCpuExpect() + "个");
            System.out.println("预期使用内存" + ins.getMemExpect() + "MB");
            System.out.println("预期使用硬盘" + ins.getHdExpect() + "GB");
            System.out.println("预期使用带宽" + ins.getNwExpect() + "MB");
        }
        else
        {
            System.out.println("缩容模组id:" + ins.getModelId());
            System.out.println("预期释放CPU" + ins.getCpuExpect() + "个");
            System.out.println("预期释放内存" + ins.getMemExpect() + "MB");
            System.out.println("预期释放硬盘" + ins.getHdExpect() + "GB");
            System.out.println("预期释放带宽" + ins.getNwExpect() + "MB");
        }

    }

}
