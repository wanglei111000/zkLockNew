package org.zklock;

import org.I0Itec.zkclient.IZkDataListener;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZKHighPerfLock extends AbstractLock{
    private static final String PATH = "/zklocks";
    //当前节点路径
    private String currentPath;
    //前一个节点的路径
    private String beforePath;

    private CountDownLatch countDownLatch = null;

    //构造函数 初始化锁父节点
    public ZKHighPerfLock() {
        //如果不存在这个节点，则创建持久节点
        if (!zkClient.exists(PATH)) {
            zkClient.createPersistent(PATH);
        }
    }


    @Override
    public void releaseLock() {
        if (null != zkClient) {
            zkClient.delete(currentPath);
            zkClient.close();
        }
    }


    @Override
    public boolean tryLock() {
        //如果currentPath为空则为第一次尝试加锁，第一次加锁赋值currentPath
        if (null == currentPath || "".equals(currentPath)) {
            //在path下创建一个临时的顺序节点
            currentPath = zkClient.createEphemeralSequential(PATH+"/", "lock");
        }
        //获取所有的临时节点，并排序
        List<String> childrens = zkClient.getChildren(PATH);
        Collections.sort(childrens);  //排序  默认升序
        // 判断 当前节点 是不是与 zk 查询出来的集合中顺序最小的 相等
        //如果相等 则获取锁成功 ，不相等 则要监听当前节点的前一个节点
        if (currentPath.equals(PATH+"/"+childrens.get(0))) {
            return true;
        }else {//如果当前节点不是排名第一，则获取它前面的节点名称，并赋值给beforePath
            int pathLength = PATH.length();
            int wz = Collections.binarySearch(childrens, currentPath.substring(pathLength+1));
            beforePath = PATH+"/"+childrens.get(wz-1); //
        }
        return false;

    }

    @Override
    public void waitLock() {
        // 创建监听器
        IZkDataListener lIZkDataListener = new IZkDataListener() {
           //监听节点删除事件
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                if (null != countDownLatch){
                    countDownLatch.countDown();
                }
            }
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {

            }
        };

        //监听前一个节点的变化
        zkClient.subscribeDataChanges(beforePath, lIZkDataListener);
        if (zkClient.exists(beforePath)) {
            countDownLatch = new CountDownLatch(1);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        zkClient.unsubscribeDataChanges(beforePath, lIZkDataListener);
    }

}
