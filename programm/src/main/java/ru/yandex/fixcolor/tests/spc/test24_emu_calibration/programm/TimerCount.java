package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

public class TimerCount {
    @FunctionalInterface
    public interface CallBack {
        void execute(boolean flag);
    }
    // ---
    private CallBack callBack;
    private int countMax;
    private int countTik;
    private volatile int count;
    private Object countLock = new Object();
    private Thread thread;
    private String name;
    private boolean threadOn;
    private boolean fPusk;
    // ---

    public TimerCount(CallBack callBack, int countMax, int countTik, String name) {
        this.callBack = callBack;
        this.countMax = countMax;
        this.countTik = countTik;
        this.name = name;
        thread = null;
        threadOn = false;
        fPusk = false;
        thread = new Thread(this::timer_run, name);
        thread.start();
    }
    public void pusk() {
        fPusk = true;
    }
    public void stop() {
        fPusk = false;
    }
    public void close() {
        if (thread == null) return;
//        synchronized (countLock)
        {
            threadOn = false;
        }
        while (thread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //execute(false);
    }
    public void updateCount() {
        this.count = countMax;
    }
    private void execute(boolean flag) {
        if (callBack == null) return;
        callBack.execute(flag);
    }
    private void timer_run() {
        threadOn = true;
        try {
            while (threadOn) {
                Thread.sleep(countTik);
                if (fPusk) timer_execute();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            threadOn = false;
        }
    }
    private void timer_execute() {
        if (count > 0) {
            count--;
            execute(true);
        } else {
            execute(false);
        }
    }
}
