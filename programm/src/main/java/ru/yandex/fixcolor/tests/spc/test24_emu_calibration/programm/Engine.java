package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

public class Engine {
    public interface CallBack {
        void endBlock(int currentPhase);
        void sendCurrentData(int currentTime, double currentDistance, double weight);
        double getWeight(int phase, double currentDistance, double sourceDistance, double targetDistance);
    }
    public static final int PHASE_SKEEP = 0;
    public static final int PHASE_START = 1;
    public static final int PHASE_SHELF = 2;
    public static final int PHASE_PRBC  = 3;
    public static final int PHASE_BACK  = 4;
    public static final int PHASE_DELAY = 5;

    private CallBack callBack;

    private double sourceDistance;
    private double targetDistance;
    private double subDistance;
    private int sourceTime;
    private int targetTime;
    private int subTime;
    // ---
    private int targetPhase;
    private int blockTime;
    // ---
    private DataEmuUnit[] dataEmu;
    private int dataEmuIdx;
    private int currentTime;
    private double currentDistance;

    public void setData(DataEmuUnit[] dataEmu) {
        this.dataEmu = dataEmu;
        dataEmuIdx = 0;
        sourceTime = 0;
        blockTime = dataEmu[dataEmu.length - 1].time;
        sourceDistance = targetDistance = dataEmu[0].distance;
        // для первичной загрузки
        currentTime = -100;
    }

    public void timer_execution() {
        if (currentTime > blockTime) return;
        if (currentTime < 0) {
            currentTime = 0;
            loadBlock();
        }
        if (currentTime > targetTime) loadBlock();
        int time = currentTime - sourceTime;
        double distance = ((double) time / (double) subTime) * subDistance;
        currentDistance = distance + sourceDistance;
        if ((currentTime % 5) == 0) sendCurrentData(currentTime, currentDistance);
        if (currentTime >= targetTime) endBlock();
        currentTime++;
    }

    private void loadBlock() {
        sourceTime = targetTime;
        sourceDistance = targetDistance;
        targetTime = dataEmu[dataEmuIdx].time;
        targetDistance = dataEmu[dataEmuIdx].distance;
        targetPhase = dataEmu[dataEmuIdx].phase;
        //
        subTime = targetTime - sourceTime;
        subDistance = targetDistance - sourceDistance;
        //
        dataEmuIdx++;
    }

    private void endBlock() {
        if (targetPhase == PHASE_SKEEP) return;
        callBack.endBlock(targetPhase);
    }

    public void on() {
    }

    public void off() {
    }
    void sendCurrentData(int currentTime, double currentDistance) {
        if (targetPhase == PHASE_DELAY) return;
        double subTargetDistance = 0;
        if (targetPhase == PHASE_START) subTargetDistance = Math.abs(targetDistance - currentDistance);
        if (targetPhase == PHASE_PRBC)  subTargetDistance = Math.abs(sourceDistance - currentDistance);
        if (targetPhase == PHASE_BACK)  subTargetDistance = Math.abs(targetDistance - currentDistance);
        double weight = callBack.getWeight(targetPhase, currentDistance, sourceDistance, targetDistance);
        callBack.sendCurrentData(currentTime, currentDistance, weight);
    }
    public Engine(CallBack callBack) {
        this.callBack = callBack;
    }

    public int getCurrentTime() {
        return currentTime < 0 ? 0 : currentTime;
    }
}
