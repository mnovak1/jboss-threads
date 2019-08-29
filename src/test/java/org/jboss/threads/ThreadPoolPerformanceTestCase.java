/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.threads;

import org.jboss.logging.Logger;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadPoolPerformanceTestCase {

    private static final Logger log = Logger.getLogger(ThreadPoolPerformanceTestCase.class);

    @Test
    public void perfTestEnhacedThreadQueueExecutor() throws Exception {
        EnhancedQueueExecutor executor = (new EnhancedQueueExecutor.Builder())
                .setKeepAliveTime(60, TimeUnit.SECONDS)
                .allowCoreThreadTimeOut(false)
                .setCorePoolSize(10)
                .setMaximumPoolSize(10)
                .build();
        runPerfTest(executor);
        executor.shutdown();
    }

    @Test
    public void perfTestThreadPoolExecutor() throws Exception {
        JBossThreadPoolExecutor executor = new JBossThreadPoolExecutor(10, 10, 60,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        executor.allowCoreThreadTimeOut(false);
        runPerfTest(executor);
        executor.shutdown();
    }


    private void runPerfTest(Executor executor) throws Exception {
        long numberOfTasks = 100000000;
        long startTime = System.currentTimeMillis();
        for (long i = 0; i < numberOfTasks; i++) {
            executor.execute(new EmptyTask());
        }

        waitProcessedTasks(executor, numberOfTasks, 600000);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        float throughput = ((float) numberOfTasks / (totalTime / 1000F));
        log.info("It took: " + totalTime + " ms to finish " + numberOfTasks + " tasks which is thoughput of : " + throughput + " tasks/second");
    }

    // There is problem that EnhancedQueueExecutor does not implement ThreadPoolExecutor interface thus this mess
    private void waitProcessedTasks(Executor executor, long processedTasks, long waitMillis) throws Exception {
        long deadline = System.currentTimeMillis() + waitMillis;
        long delayMillis = 100;

        do {
            if (executor instanceof EnhancedQueueExecutor &&
                    ((EnhancedQueueExecutor) executor).getCompletedTaskCount() == processedTasks) {
                break;
            }
            if (executor instanceof JBossThreadPoolExecutor &&
                    ((JBossThreadPoolExecutor) executor).getCompletedTaskCount() == processedTasks) {
                break;
            }
            Thread.sleep(delayMillis);
        } while (System.currentTimeMillis() + delayMillis < deadline);
    }

    class EmptyTask implements Runnable {

        @Override
        public void run() {
        }
    }
}

