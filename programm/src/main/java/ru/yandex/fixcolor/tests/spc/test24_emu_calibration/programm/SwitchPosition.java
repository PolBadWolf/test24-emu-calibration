package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

public enum SwitchPosition {
    STOP        (0),
    ONE         (1),
    MULTI       (2);
    private int position;

    SwitchPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
