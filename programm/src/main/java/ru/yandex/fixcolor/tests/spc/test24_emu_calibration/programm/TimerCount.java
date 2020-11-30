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
    private int countTikCount;
    private volatile int count;
    private Object countLock = new Object();
    private String name;
    private boolean fPusk;
    // ---

    public TimerCount(CallBack callBack, int countMax, int countTik, String name) {
        this.callBack = callBack;
        this.countMax = countMax;
        this.countTik = countTik;
        countTikCount = countTik;
        this.name = name;
        fPusk = false;
    }
    public void pusk() {
        fPusk = true;
    }
    public void stop() {
        fPusk = false;
    }
    public void updateCount() {
        this.count = countMax;
    }
    private void execute(boolean flag) {
        if (callBack == null) return;
        callBack.execute(flag);
    }
    public void timer_execute() {
        countTikCount--;
        if (countTikCount > 0) return;
        countTikCount = countTik;
        if (count > 0) {
            count--;
            execute(true);
        } else {
            execute(false);
        }
    }
}
