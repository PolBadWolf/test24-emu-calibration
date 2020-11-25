package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232;

public interface CommPort {
    interface CallBack {
        void reciveRsPush(byte[] bytes, int lenght);
    }
    static CommPort init() {
        return new CommPortClass();
    }
    static String[] getListPortsName() { return CommPortClass.getListPortsName(); }
    static boolean isCheckCommPort(String portName) throws Exception { return CommPortClass.isCheckCommPort(portName); }

    PortStat open(CallBack callBack, String portName, BAUD baud);
    int INITCODE_OK           = 0;
    int INITCODE_NOTEXIST     = 1;
    int INITCODE_ERROROPEN    = 2;
    int UNKNOWN_ERRORN        = 99;
    enum PortStat {
        INITCODE_OK         (CommPort.INITCODE_OK),
        INITCODE_NOTEXIST   (CommPort.INITCODE_NOTEXIST),
        INITCODE_ERROROPEN  (CommPort.INITCODE_ERROROPEN),
        UNKNOWN_ERRORN      (CommPort.UNKNOWN_ERRORN);
        int portStat;
        PortStat(int portStat) {
            this.portStat = portStat;
        }
        public int getCodePortStat() {
            return portStat;
        }
        @Override
        public String toString() {
            String stat = "";
            switch (portStat) {
                case CommPort.INITCODE_OK:
                    stat = "INITCODE_OK";
                    break;
                case CommPort.INITCODE_NOTEXIST:
                    stat = "INITCODE_NOTEXIST";
                    break;
                case CommPort.INITCODE_ERROROPEN:
                    stat = "INITCODE_ERROROPEN";
                    break;
                default:
                    stat = "UNKNOWN_ERRORN";
            }
            return stat;
        }
    }

    void close();
    boolean ReciveStart();
    void ReciveStop();
    void sendMessageStopAuto();
}
