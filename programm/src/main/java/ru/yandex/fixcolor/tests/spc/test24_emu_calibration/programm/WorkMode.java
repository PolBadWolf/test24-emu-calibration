package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

public enum WorkMode {
    manual_alarm    (0),
    manual_back     (1),
    manual_stop     (2),
    manual_pusk     (3),
    manual_shelf    (4),
    cycle_alarm     (5),
    cycle_back      (6),
    cycle_delay     (7),
    cycle_pusk      (8),
    cycle_shelf     (9);


    private int mode;

    WorkMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
