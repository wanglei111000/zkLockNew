package org.zklock;

public class LockTest02 {
    public static void main(String[] args) {
        //模拟多个10个客户端
        for (int i=0;i<10;i++) {
            Thread thread = new Thread(new LockTest02.LockRunnable());
            thread.start();
        }

    }


    static class LockRunnable implements Runnable{
        @Override
        public void run() {
            AbstractLock zkLock = new ZKHighPerfLock();
            zkLock.getLock();
            try {
                //模拟业务操作
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                zkLock.releaseLock();
                e.printStackTrace();
            }
            zkLock.releaseLock();
        }

    }
}
