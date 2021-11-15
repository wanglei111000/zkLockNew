package org.zklock;

import org.I0Itec.zkclient.IZkDataListener;

import java.util.concurrent.CountDownLatch;

public class SimpleZkLock extends AbstractLock {
    private static final String NODE_NAME = "/simple_lock";
    private CountDownLatch countDownLatch;


    @Override
    public void releaseLock() {
        if (null != zkClient) {
            //删除节点
            zkClient.delete(NODE_NAME);
            zkClient.close();
            System.out.println(Thread.currentThread().getName()+"-释放锁成功");
        }
    }

    @Override
    public boolean tryLock() {
        if (null == zkClient) return false;
        try {
            zkClient.createEphemeral(NODE_NAME);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void waitLock() {
        //创建监听器
        IZkDataListener iZkDataListener = new IZkDataListener() {
            //节点被删除回调
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                }
            }

            //节点改变被回调
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                // TODO Auto-generated method stub

            }
        };
        //订阅数据变更监听事件
        zkClient.subscribeDataChanges(NODE_NAME, iZkDataListener);
       //如果存在节点则阻塞
        if (zkClient.exists(NODE_NAME)) {
            countDownLatch = new CountDownLatch(1);
            try {
                countDownLatch.await();
                System.out.println(Thread.currentThread().getName()+" 等待获取锁...");
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //删除监听
        zkClient.unsubscribeDataChanges(NODE_NAME, iZkDataListener);
    }
}
