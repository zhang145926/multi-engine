package com.baidu.unbiz.multiengine.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.baidu.unbiz.multiengine.common.DisTaskPair;
import com.baidu.unbiz.multiengine.transport.EndpointPool;
import com.baidu.unbiz.multiengine.transport.client.SendFuture;
import com.baidu.unbiz.multiengine.transport.client.TaskClient;
import com.baidu.unbiz.multitask.common.TaskPair;
import com.baidu.unbiz.multitask.constants.TaskConfig;
import com.baidu.unbiz.multitask.exception.TaskTimeoutException;
import com.baidu.unbiz.multitask.policy.ExecutePolicy;
import com.baidu.unbiz.multitask.task.SimpleParallelExePool;
import com.baidu.unbiz.multitask.task.thread.TaskContext;

/**
 * Created by wangchongjie on 16/4/14.
 */
@Component
public class DistributedParallelExePool extends SimpleParallelExePool {

    private final String DIS_TASK_PAIRS = "disTaskPairs";
    private final String FUTURES = "futures";

    private void dispatchTaskPairs(List<TaskPair> localTaskPairs, List<TaskPair> disTaskPairs, TaskPair... taskPairs) {
        for (TaskPair taskPair : taskPairs) {
            if (taskPair instanceof DisTaskPair) {
                disTaskPairs.add(taskPair);
            } else {
                localTaskPairs.add(taskPair);
            }
        }
    }

    public TaskContext beforeSubmit(TaskContext context, ExecutePolicy policy, TaskPair... taskPairs) {
        List<TaskPair> localTaskPairs = new ArrayList<TaskPair>();
        List<TaskPair> disTaskPairs = new ArrayList<TaskPair>();
        dispatchTaskPairs(localTaskPairs, disTaskPairs, taskPairs);

        TaskPair[] taskPairsArray = null;
        if(CollectionUtils.isNotEmpty(localTaskPairs)) {
            taskPairsArray = localTaskPairs.toArray(new TaskPair[]{});
        }
        return context.putAttribute(TASK_PAIRS, taskPairsArray).putAttribute(DIS_TASK_PAIRS, disTaskPairs);
    }

    public TaskContext onSubmit(TaskContext context, ExecutePolicy policy, TaskPair... taskPairs) {
        Map<String, SendFuture> futures = new HashMap<String, SendFuture>();
        List<TaskPair> disTaskPairs = context.getAttribute(DIS_TASK_PAIRS);
        if (CollectionUtils.isEmpty(disTaskPairs)) {
            return context;
        }
        for (TaskPair taskPair : disTaskPairs) {
            TaskCommand command = new TaskCommand(taskPair);
            TaskClient taskClient = EndpointPool.selectEndpoint();
            futures.put(taskPair.field1, taskClient.asynCall(command));
        }
        return context.putAttribute(FUTURES, futures);
    }

    public TaskContext postSubmit(TaskContext context, ExecutePolicy policy, TaskPair... taskPairs) {
        Map<String, SendFuture> futures = context.getAttribute(FUTURES);
        for (Map.Entry<String, SendFuture> future : futures.entrySet()) {
            Object result;
            if (TaskConfig.NOT_LIMIT == policy.taskTimeout()) {
                result = future.getValue().get();
            } else {
                try {
                    result = future.getValue().get(policy.taskTimeout(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new TaskTimeoutException(e);
                }
            }
            context.putResult(future.getKey(), result);
        }
        return context;
    }

}
