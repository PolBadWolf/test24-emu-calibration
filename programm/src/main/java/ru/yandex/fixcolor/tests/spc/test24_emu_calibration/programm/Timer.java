package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

public class Timer {
    private Thread thread;
    private volatile boolean threadOn;
    private boolean suspended;
    private CallBack callBack;
    public interface CallBack {
        void timerTik();
    }

    public Timer(CallBack callBack) {
        init(callBack);
    }

    public void init(CallBack callBack) {
        this.callBack = callBack;
        init();
    }

    public void init() {
        suspended = true;
        thread = new Thread(this::timerRun, "timer");
        thread.start();
    }

    private void timerRun() {
        threadOn = true;
        try {
            while (threadOn) {
                Thread.sleep(1);
                if (suspended) continue;
                if (callBack != null) callBack.timerTik();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            threadOn = false;
        }
    }
    public void close() {
        threadOn = false;
        while (thread.isAlive()) Thread.yield();
        thread = null;
    }

    public void start() {
        suspended = false;
    }

    public void suspended() {
        suspended = true;
    }
}
