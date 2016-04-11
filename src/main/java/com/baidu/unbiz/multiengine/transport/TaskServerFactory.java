package com.baidu.unbiz.multiengine.transport;

/**
 * Created by wangchongjie on 16/4/11.
 */
public class TaskServerFactory {

    public static TaskServer createTaskServer(HostConf hostConf) {
        TaskServer taskServer = new TaskServer();
        taskServer.setHostConf(hostConf);
        return taskServer;
    }
}
