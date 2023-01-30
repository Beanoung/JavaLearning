import java.util.concurrent.*;

/**
 * ---线程的创建---
 * 1.继承Thread类: 重写run方法,start启动
 * 2.实现Runnable接口: 同上
 * 3.匿名内部类: 同上(分为 Thread、Runnable、FutureTask 三种)
 * 4.实现Callable接口,创建FutureTask
 * 5.ForkJoinPool 线程池 (RecursiveTask 和 RecursiveAction)
 * 6.ThreadPoolExecutor 工具类,可以直接创建一个线程池
 * 7.并发包 Executors,调用方法可以创建6种线程池
 *      1.单线程线程池：Single Thread Executor : 只有一个线程的线程池，因此所有提交的任务是顺序执行;
 *      2.缓存线程池：Cached Thread Pool : 线程池里有很多线程需要同时执行，老的可用线程将被新的任务触发重新执行，如果线程超过60秒内没执行，那么将被终止并从池中删除;
 *      3.固定线程数线程池：Fixed Thread Pool : 拥有固定线程数的线程池，如果没有任务执行，那么线程会一直等待;
 *      4.周期性线程的线程池 : Scheduled Thread Pool : 用来调度即将执行的任务的线程池;
 *      5.单线程周期性线程池：Single Thread Scheduled Pool : 只有一个线程，用来调度执行将来的任务;
 *      6.工作窃取池：Work Stealing Pool : 适合使用在很耗时的任务中,底层用的ForkJoinPool 实现
 */

public class MyThread extends Thread {
    public static void main(String[] args) {
        //继承Thread类重写run方法的线程.start
        MyThread myThread = new MyThread();
        // myThread.start();        //这里start的话,74行的join不好观察执行顺序

        //线程1,Thread匿名内部类
        new Thread("偶数") {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i++) {
                    if (i % 2 == 0) {
                        System.out.println(getName() + "：" + i);
                    }
                }
            }
        }
        // .start()
        ;

        //线程2,Runnable匿名内部类,可以声明一个变量(方便join)
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i++) {
                    if (i % 2 != 0) {
                        System.out.println(currentThread().getName() + "：" + i);
                    }
                    if (i == 99) {
                        System.out.println("-----奇数 输出完毕-----");
                    }
                }
            }
        },"奇数");
        //可以使用lambda表达式
        /* Thread thread = new Thread(() -> {
            for (int i = 0; i <= 100; i++) {
                if (i % 2 != 0) {
                    System.out.println(currentThread().getName() + "：" + i);
                }
                if (i == 99) {
                    System.out.println("-----奇数 输出完毕-----");
                }
            }
        },"奇数"); */
        // thread.setName("奇数");
        thread.start();

        //线程3,匿名内部类,测试join()方法
        new Thread() {
            @Override
            public void run() {
                super.setName("字母");
                for (char i = 'A'; i <= 'z'; i++) {
                    System.out.println(getName() + "：" + i);
                    if (i == 'Z') {
                        System.out.println("-----大写字母 输出完毕-----");
                        i += 6;   //有6个符号
                        // this.yield();
                        try {
                            sleep(100);
                            // myThread.start();
                            thread.join();
                            // myThread.join();         //这里join没差别,因为之前myThread就start了,thread join并不影响myThread
                            myThread.start();           //但,如果在这里start,就要等thread 执行完,才会执行下面的代码
                            myThread.join();            //这里不join,该线程就执行完了,不方便观察-----测试执行顺序
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }.start();

        /**
         * 测试几个常见线程,main主线程、gc线程、异常处理线程
         */
        //主方法线程
        System.out.println("-----main:" + Thread.currentThread().getName());

        //gc线程
        new MyThread();//匿名对象,会被回收,finalize()就会被调用
        System.gc();//调用gc回收

        //线程5,实现Runnable接口,重写run方法
        Thread threadB = new Thread(new RunnableThread(), "RunnableThread");
        threadB.start();

        //线程6,实现Callable接口
        CallableThread callableThread=new CallableThread();
        FutureTask<Object> futureTask=new FutureTask<>(callableThread);
        futureTask.run();
        Thread thread1=new Thread(futureTask,"CallableThread");
        thread1.start();

        //线程7,匿名内部类 使用 FutureTask和Callable
        new Thread(new FutureTask<>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                System.out.println("-----FutureTaskThread:"+currentThread().getName());
                return null;
            }
        }),"FutureTaskThread").start();

        //ForkJoinPool 线程池
        new ForkJoinPool().submit(new RecursiveTaskThread());

        new ForkJoinPool().submit(new RecursiveActionThread());

        //ThreadPoolExecutor 线程池
        ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(1,1,
                2,TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
        threadPoolExecutor.execute(new RunnableThread());

        //并发包 Executors     太多了,看最上面的注释吧

    }

    //线程4,继承Thread类,重写run方法
    @Override
    public void run() {
        for (int i = 0; i < 50; i++) {
            System.out.println(getName() + "：MyThread" + i);
        }
    }

    //用于输出gc线程信息
    @Override
    protected void finalize() throws Throwable {
        try {
            System.out.println("-----gc:" + currentThread().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            super.finalize();
        }
    }
}

//实现Runnable接口,避免单继承
class RunnableThread implements Runnable {
    @Override
    public void run() {
        System.out.println("-----RunnableThread:"+Thread.currentThread().getName());
    }
}

//使用Callable和FutureTask: 执行call方法并且有返回值
class CallableThread implements Callable {
    @Override
    public Object call() throws Exception {
        System.out.println("-----CallableThread:"+Thread.currentThread().getName());
        return null;
    }
}

/**
 * 下面两个是Java7 提供的 Fork/Join框架: 把大任务分割成若干个小任务，最终汇总每个小任务结果后得到大任务结果的框架.
 */
//RecursiveTask 有返回值
class RecursiveTaskThread extends RecursiveTask{
    @Override
    protected Object compute() {
        System.out.println("-----RecursiveTaskThread:"+Thread.currentThread().getName());
        return null;
    }
}

//RecursiveAction 无返回值
class RecursiveActionThread extends RecursiveAction {
    @Override
    protected void compute() {
        System.out.println("-----RecursiveActionThread:"+Thread.currentThread().getName());
    }
}