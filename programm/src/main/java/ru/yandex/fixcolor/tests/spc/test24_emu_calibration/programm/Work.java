package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

import ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232.CommPort;

import java.util.ArrayList;

public class Work {
    private static final int mSTOP = 0;
    private static final int mPUSK = 1;
    private static final int mCYCL = 2;
    private static final int mNONE = 3;
    private static final int mSET = 4;
    private WorkMode mode;
    private Engine engine;
    private MainClass main;
    private CommPort commPort;
    //--
    private SwitchPosition switchPos;
    private boolean switchEvent;
    // --------------------
    private int alrmTimeOut;
    private static final int alrmTimeOutMax = 2_500;
    private int cycleWork;
    private static final int cycleWorkMax = 15;
    // --------------------
    private static final double g_std = 9.8;
    private double g_up;
    private double g_dn;
    private static final double distanceForce = 100.0;
    // --------------------
    private double forcePusher;
    private double weightPusher;
    // --------------------
    // флаг стоп из компьютера
    private boolean flagStopFromPc;

    public Work(CommPort commPort) {
        main = MainClass.main;
        mode = WorkMode.manual_stop;
        engine = new Engine(new EngineCallBack());
        this.commPort = commPort;
        weightPusher = 20.0;
        forcePusher = 350.0;
        loadVars(true);
    }

    public void init() {
        setMode(WorkMode.manual_stop);
        switchEvent = false;
    }

    private void setMode(WorkMode mode) {
        fns[mode.getMode()][mSET].method();
    }

    public void timer_execute() {
        int path;
        if (switchEvent) {
            switchEvent = false;
            switch (switchPos) {
                case STOP:
                    path = mSTOP;
                    break;
                case ONE:
                    path = mPUSK;
                    break;
                case MULTI:
                    path = mCYCL;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + switchPos);
            }
        } else {
            path = mNONE;
        }
        fns[mode.getMode()][path].method();
        if (mode == WorkMode.manual_stop) return;
        if (mode == WorkMode.manual_alarm) return;
        if (mode == WorkMode.cycle_alarm) return;
        engine.timer_execution();
    }

    public void resetMode() {
        mode = WorkMode.manual_stop;
    }
    public void switchStop() {
        switchPos = SwitchPosition.STOP;
        switchEvent = true;
    }
    public void switchOne() {
        switchPos = SwitchPosition.ONE;
        switchEvent = true;
    }
    public void switchMulti() {
        switchPos = SwitchPosition.MULTI;
        switchEvent = true;
    }

    private final IntFn[][] fns = new IntFn[][] {
            // manual alarm
            {this::dump_stop,   this::dump_pusk,    this::dump_cycl,    this::mAlarm_none, this::mAlarm_set},
            // manual back
            {this::dump_stop,   this::dump_pusk,    this::dump_cycl,    this::mBack_none,   this::mBack_set},
            // manual stop
            {this::dump_stop,   this::mPusk_set,    this::cPusk_set,    this::dump_none,   this::mStop_set},
            // manual pusk
            {this::mPusk_stop,  this::dump_pusk,    this::mPusk_cycl,   this::mPusk_none,   this::mPusk_set},
            // manual shelf
            {this::mBack_set,   this::dump_pusk,    this::dump_cycl,    this::dump_none,    this::mShelf_set},
            // cycle alarm
            {this::dump_stop,   this::dump_pusk,    this::dump_cycl,    this::cAlarm_none,  this::cAlarm_set},
            // cycle back
            {this::dump_stop,   this::dump_pusk,    this::dump_cycl,    this::cBack_none,   this::cBack_set},
            // cycle delay
            {this::mStop_set,   this::dump_pusk,    this::dump_cycl,    this::cDelay_none,  this::cDelay_set},
            // cycle pusk
            {this::cPusk_stop,  this::cPusk_pusk,   this::dump_cycl,    this::cPusk_none,   this::cPusk_set},
            // cycle shelf
            {this::mStop_set,   this::dump_pusk,    this::dump_cycl,    this::cShelf_none,  this::cShelf_set},
            {this::dump_stop,   this::dump_pusk,    this::dump_cycl,    this::dump_none,    this::dump_set}
    };

    // -----------------------------------
    private void loadVars(boolean one) {
        main.distance_shelf_distance.setText(main.distance_start_distance.getText());
        main.distance_delay_distance.setText(main.distance_stop_distance.getText());
        ArrayList<DataEmuUnit> dataEmu = new ArrayList<>();
        int time = 0;
        // начало
        time += Integer.parseInt(main.distance_begin_time.getText());
        dataEmu.add(new DataEmuUnit(Engine.PHASE_SKEEP, time, Double.parseDouble(main.distance_begin_distance.getText())));
        // страрт
        time += Integer.parseInt(main.distance_start_time.getText());
        dataEmu.add(new DataEmuUnit(Engine.PHASE_START, time, Double.parseDouble(main.distance_start_distance.getText())));
        // полка
        time += Integer.parseInt(main.distance_shelf_time.getText());
        dataEmu.add(new DataEmuUnit(Engine.PHASE_SHELF, time, Double.parseDouble(main.distance_shelf_distance.getText())));
        // назад
        time += Integer.parseInt(main.distance_back_time.getText());
        dataEmu.add(new DataEmuUnit(Engine.PHASE_PRBC, time, Double.parseDouble(main.distance_back_distance.getText())));
        // стоп
        time += Integer.parseInt(main.distance_stop_time.getText());
        dataEmu.add(new DataEmuUnit(Engine.PHASE_BACK, time, Double.parseDouble(main.distance_stop_distance.getText())));
        if (!one) {
            // пауза
            time += Integer.parseInt(main.distance_delay_time.getText());
            dataEmu.add(new DataEmuUnit(Engine.PHASE_DELAY, time, Double.parseDouble(main.distance_delay_distance.getText())));
        }
        engine.setData(dataEmu.toArray(new DataEmuUnit[0]));
        g_up = g_std + renderAcceleration(main.distance_begin_distance.getText(), main.distance_start_distance.getText(), main.distance_start_time.getText());
        g_dn = g_std - renderAcceleration(main.distance_shelf_distance.getText(), main.distance_back_distance.getText(),  main.distance_back_time.getText());
        //
        weightPusher = Double.parseDouble(main.weightPusherText.getText());
        forcePusher = Double.parseDouble(main.forcePusherText.getText());;
    }

    private double renderAcceleration(String dist1Text, String dist2Text, String timeText) {
        double dist1 = Double.parseDouble(dist1Text);
        double dist2 = Double.parseDouble(dist2Text);
        int time = Integer.parseInt(timeText);
        double g = (dist1 - dist2) / time;
        return Math.abs(g);
    }

    // -----------------------------------
    private void outWorkStatus(String workStatus) {
        main.statusText.setText(workStatus + ":" + cycleWork);
        System.out.println("work status: " + workStatus + ":" + cycleWork);
    }
    private void sendStatus(int typePack, int currentTime) {
        try {
            commPort.sendStatus(typePack, currentTime);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    // -----------------------------------
    private void mAlarm_set() {
        mode = WorkMode.manual_alarm;
        outWorkStatus("mAlarm");
        sendStatus(TypePack.MANUAL_ALARM, engine.getCurrentTime());
        alrmTimeOut = alrmTimeOutMax;
    }

    private void mAlarm_none() {
        if (alrmTimeOut > 0) alrmTimeOut--;
        else setMode(WorkMode.manual_stop);
    }

    // -----------------------------------
    private void mBack_set() {
        mode = WorkMode.manual_back;
        outWorkStatus("mBack");
        sendStatus(TypePack.MANUAL_BACK, engine.getCurrentTime());
        // отключить двигатель
        engine.off();
    }
    // заглушка
    private void mBack_none() {
    }
    // -----------------------------------
    private void mStop_set() {
        mode = WorkMode.manual_stop;
        outWorkStatus("stop");
        sendStatus(TypePack.MANUAL_STOP, engine.getCurrentTime());
        // отключить двигатель
        engine.off();
        // сброc счетчика циклов
        cycleWork = 0;
    }
    // -----------------------------------
    private void mPusk_set() {
        mode = WorkMode.manual_pusk;
        outWorkStatus("mPusk");
        // загрузка переменных
        loadVars(true);
        sendStatus(TypePack.MANUAL_FORWARD, engine.getCurrentTime());
        sendDataWeight(engine.getCurrentTime(), (int) weightPusher);
        // флаг остановки по команде из компьютера
        flagStopFromPc = false;
        //
        // включить двигатель
        engine.on();
    }
    // аварийный режим - ключ на стоп
    private void mPusk_stop() {
        // отключить двигатель
        engine.off();
        // режим alarm
        setMode(WorkMode.manual_alarm);
    }
    // невозможный вариант - ключ на цикл
    private void mPusk_cycl() {
        // отключить двигатель
        engine.off();
        // режим alarm
        setMode(WorkMode.manual_alarm);
    }

    private void mPusk_none() {
    }
    // -----------------------------------
    private void mShelf_set() {
        mode = WorkMode.manual_shelf;
        outWorkStatus("mShelf");
        sendStatus(TypePack.MANUAL_SHELF, engine.getCurrentTime());
        //
    }
    // -----------------------------------
    private void cAlarm_set() {
        mode = WorkMode.cycle_alarm;
        outWorkStatus("cAlarm");
        sendStatus(TypePack.CYCLE_ALARM, engine.getCurrentTime());
        alrmTimeOut = alrmTimeOutMax;
    }
    private void cAlarm_none() {
        if (alrmTimeOut > 0) alrmTimeOut--;
        else setMode(WorkMode.manual_stop);
    }
    // -----------------------------------
    private void cBack_set() {
        mode = WorkMode.cycle_back;
        outWorkStatus("cBack");
        sendStatus(TypePack.CYCLE_BACK, engine.getCurrentTime());
        // отключить двигатель
        engine.off();
    }
    // заглушка
    private void cBack_none() {
    }
    // -----------------------------------
    private void cDelay_set() {
        mode = WorkMode.cycle_delay;
        outWorkStatus("cDelay");
        sendStatus(TypePack.CYCLE_DELAY, engine.getCurrentTime());
        // отключить двигатель
        engine.off();
        //
        if (cycleWork >= cycleWorkMax) {
            System.out.println("конец циклов");
            setMode(WorkMode.manual_stop);
        }
    }
    private void cDelay_none() {
        if (flagStopFromPc) {
            setMode(WorkMode.manual_stop);
        }
    }
    // -----------------------------------
    private void cPusk_set() {
        mode = WorkMode.cycle_pusk;
        cycleWork++;
        outWorkStatus("cPusk");
        // загрузка переменных
        loadVars(false);
        sendStatus(TypePack.CYCLE_FORWARD, engine.getCurrentTime());
        sendDataWeight(engine.getCurrentTime(), (int) weightPusher);
        // флаг остановки по команде из компьютера
        flagStopFromPc = false;
        //
        // включить двигатель
        engine.on();
    }
    // аварийное завершение
    private void cPusk_stop() {
        // отключить двигатель
        engine.off();
        // режим alarm
        setMode(WorkMode.cycle_alarm);
    }
    // невозмоная ситуация
    private void cPusk_pusk() {
        // отключить двигатель
        engine.off();
        // режим alarm
        setMode(WorkMode.cycle_alarm);
    }
    // заглушка
    private void cPusk_none() {
    }
    // -----------------------------------
    private void cShelf_set() {
        mode = WorkMode.cycle_shelf;
        outWorkStatus("cShelf");
        sendStatus(TypePack.CYCLE_SHELF, engine.getCurrentTime());
    }
    // заглушка
    private void cShelf_none() {
    }
    // -----------------------------------


    public void setFlagStopFromPc() {
        this.flagStopFromPc = true;
    }
    private void sendDataWeight(long tik, int weight) {
        int ves_adc = ((int) Math.round((weight - main.ves_offset) / main.ves_multiply)) & 0x03ff;
        try {
            commPort.sendDataWeight((byte) TypePack.WEIGHT, tik, ves_adc);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // заглушка
    private void dump_stop() { }
    private void dump_pusk() { }
    private void dump_cycl() { }
    private void dump_none() { }
    private void dump_set() { }
    @FunctionalInterface
    private interface IntFn {
        void method();
    }

    private class EngineCallBack implements Engine.CallBack {
        @Override
        public void endBlock(int currentPhase) {
            switch (mode) {
                case manual_alarm:
                case manual_back:
                case manual_stop:
                case manual_pusk:
                case manual_shelf:
                    switch (currentPhase) {
                        case Engine.PHASE_START:
                            setMode(WorkMode.manual_shelf);
                            break;
                        case Engine.PHASE_SHELF:
                            setMode(WorkMode.manual_back);
                            break;
                        case Engine.PHASE_PRBC:
                            break;
                        case Engine.PHASE_BACK:
                            setMode(WorkMode.manual_stop);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected phase: " + currentPhase);
                    }
                    break;
                // ---
                case cycle_alarm:
                case cycle_back:
                case cycle_delay:
                case cycle_pusk:
                case cycle_shelf:
                    switch (currentPhase) {
                        case Engine.PHASE_START:
                            setMode(WorkMode.cycle_shelf);
                            break;
                        case Engine.PHASE_SHELF:
                            setMode(WorkMode.cycle_back);
                            break;
                        case Engine.PHASE_PRBC:
                            break;
                        case Engine.PHASE_BACK:
                            setMode(WorkMode.cycle_delay);
                            break;
                        case Engine.PHASE_DELAY:
                            setMode(WorkMode.cycle_pusk);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected phase: " + currentPhase);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mode);
            }
        }

        @Override
        public void sendCurrentData(int currentTime, double currentDistance, double weight) {
//            System.out.print("время: " + String.format("%5d", currentTime));
//            System.out.print("\tдистанция: " + String.format("%12.3f", currentDistance));
//            System.out.print("\tусилие: " + String.format("%12.3f", weight));
//            System.out.println();
            // ---
            int dist_adc = ((int) Math.round((currentDistance - main.dist_offset) / main.dist_multiply)) & 0x03ff;
            int ves_adc = ((int) Math.round((weight - main.ves_offset) / main.ves_multiply)) & 0x03ff;
            try {
                commPort.sendDataMeasured((byte) TypePack.CURENT_DATA, currentTime, dist_adc, ves_adc);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            // ---
        }

        @Override
        public double getWeight(int phase, double currentDistance, double sourceDistance, double targetDistance) {
            double weight, weightAcceleration;
            double subDistanceOut, subDistanceInp;
            switch (phase) {
                case Engine.PHASE_START:
                    subDistanceOut = Math.abs(targetDistance - currentDistance);
                    subDistanceInp = Math.abs(sourceDistance - currentDistance);
                    weightAcceleration = (g_up / g_std) * weightPusher;
                    if (subDistanceInp < distanceForce) {
                        double subDist = subDistanceInp; //distanceForce - subDistanceInp;
                        double subWeight = weightAcceleration - weightPusher;
                        weight = ((subDist / distanceForce) * subWeight) + weightPusher;
                    } else
                    if (subDistanceOut > distanceForce) weight = weightAcceleration;
                    else {
                        double subDist = distanceForce - subDistanceOut;
                        double subWeight = (forcePusher + weightPusher) - weightAcceleration;
                        weight = ((subDist / distanceForce) * subWeight) + weightAcceleration;
                    }
                    break;
                case Engine.PHASE_SHELF:
                    weight = weightPusher + forcePusher;
                    break;
                case Engine.PHASE_PRBC:
                    subDistanceOut = Math.abs(targetDistance - currentDistance);
                    subDistanceInp = Math.abs(sourceDistance - currentDistance);
                    weightAcceleration = (g_dn / g_std) * weightPusher;
                    if (subDistanceInp < distanceForce) {
                        double subDist = distanceForce - subDistanceInp;
                        double subWeight = (forcePusher + weightPusher) - weightAcceleration;
                        weight = ((subDist / distanceForce) * subWeight) + weightAcceleration;
                    } else  if (subDistanceOut < distanceForce) {
                        double subDist = distanceForce - subDistanceOut;
                        double subWeight = weightPusher - weightAcceleration;
                        weight = ((subDist / distanceForce) * subWeight) + weightAcceleration;
                    } else {
                        weight = weightAcceleration;
                    }
                    break;
                case Engine.PHASE_BACK:
                case Engine.PHASE_SKEEP:
                    weight = weightPusher;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + phase);
            }
            return weight;
        }
    }
}

