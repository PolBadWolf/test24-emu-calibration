package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232;

import com.fazecast.jSerialComm.SerialPort;
import ru.yandex.fixcolor.library.controlsumma.ControlSumma;

class CommPortClass implements CommPort {

    private SerialPort port = null;
    private CallBack callBack = null;
    private Thread threadRS = null;
    private boolean onCycle;

    static String[] getListPortsName() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] namePorts = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            namePorts[i] = ports[i].getSystemPortName().toUpperCase();
        }
        return namePorts;
    }
    static boolean isCheckCommPort(String portName) throws Exception {
        if (portName == null) {
            throw new Exception("имя порта не установлено");
        }
        CommPortClass port = new CommPortClass();
        PortStat stat = port.open(
                (bytes, lenght) -> { },
                portName,
                BAUD.baud57600
        );
        port.close();
        if (stat == PortStat.INITCODE_OK) return true;
        return false;
    }

    @Override
    public PortStat open(CallBack callBack, String portName, BAUD baud) {
        if (port != null) {
            close();
        }

        boolean flagTmp = false;
        String[] portsName = CommPort.getListPortsName();
        String portNameCase = portName.toUpperCase();
        for (String s : portsName) {
            if (s.equals(portNameCase)) {
                flagTmp = true;
                break;
            }
        }

        if (!flagTmp)   return CommPort.PortStat.INITCODE_NOTEXIST;

        port = SerialPort.getCommPort(portNameCase);
        port.setComPortParameters(baud.getBaud(), 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 1000, 0);

        if (port.openPort()) {
            this.callBack = callBack;
            return CommPort.PortStat.INITCODE_OK;
        }

        return CommPort.PortStat.INITCODE_ERROROPEN;
    }

    @Override
    public void close() {
        if (port == null)   return;

        ReciveStop();

        port.closePort();
        port = null;
    }

    @Override
    public boolean ReciveStart() {
        if (port == null)   return false;
        if (!port.isOpen()) return false;

        threadRS = new Thread(this::runner);
        threadRS.start();
        return false;
    }

    @Override
    public void ReciveStop() {
        onCycle = false;

        try {
            if (threadRS != null) {
                while (threadRS.isAlive()) {
                    Thread.yield();
                }
            }
        }
        catch (java.lang.Throwable th) {
            th.printStackTrace();
        }
    }

    // ---------------------
    //          режим работы
    private static final int reciveMode_SYNHRO = 0;
    private static final int reciveMode_LENGHT = 1;
    private static final int reciveMode_BODY = 2;
    private static final int reciveMode_OUT = 3;
    private int reciveMode = reciveMode_SYNHRO;
    // ---------------------
    //        SYNHRO
    private static final int reciveHeader_lenght = 4;
    private byte[] reciveHeader = new byte[reciveHeader_lenght];
    private byte[] reciveHeader_in = new byte[1];
    // ---------------------
    //        LENGHT
    private int reciveBody_lenght;
    // ---------------------
    private byte[] reciveBody_Buffer = new byte[256];
    private int reciveBody_Index;
    // ---------------------
    int recive_num;

    private void runner() {
        onCycle = true;
        reciveMode = reciveMode_SYNHRO;
        recive_num = 0;
        try {
            while (onCycle) {
                if (recive_num == 0) {
                    Thread.sleep(1);
                }
                switch (reciveMode) {
                    case reciveMode_SYNHRO:
                        recive_synhro();
                        break;
                    case reciveMode_LENGHT:
                        recive_lenght();
                        break;
                    case reciveMode_BODY:
                        recive_body();
                        break;
                    case reciveMode_OUT:
                        recive_out();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + reciveMode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recive_synhro() throws Exception {
        recive_num = port.readBytes(reciveHeader_in, 1);
        if (recive_num == 0) return;
        // shift
        for (int i = 0; i < reciveHeader_lenght - 1; i++) {
            reciveHeader[i] = reciveHeader[i + 1];
        }
        reciveHeader[reciveHeader_lenght - 1] = reciveHeader_in[0];
        //
        if (reciveHeader[0] != 0xe6) return;
        if (reciveHeader[0] != 0x19) return;
        if (reciveHeader[0] != 0x55) return;
        if (reciveHeader[0] != 0xaa) return;
        reciveMode = reciveMode_LENGHT;
    }

    private void recive_lenght() throws Exception {
        recive_num = port.readBytes(reciveHeader_in, 1);
        if (recive_num == 0) return;
        reciveBody_lenght = reciveHeader_in[0];
        reciveBody_Index = 0;
        reciveMode = reciveMode_BODY;
    }

    private void recive_body() throws Exception {
        int lenght = reciveBody_lenght - reciveBody_Index;
        recive_num = port.readBytes(reciveBody_Buffer, lenght, reciveBody_Index);
        if (recive_num == 0) return;
        reciveBody_Index += recive_num;
        if (reciveBody_Index > reciveBody_lenght) throw new Exception("переполнение буфера приема");
        if (reciveBody_Index < reciveBody_lenght) return;
        reciveMode = reciveMode_OUT;
    }

    private void recive_out() {
        if (ControlSumma.crc8(reciveBody_Buffer, reciveBody_lenght - 1) == reciveBody_Buffer[reciveBody_lenght - 1]) {
            if (callBack != null) {
                callBack.reciveRsPush(reciveBody_Buffer, reciveBody_lenght);
            }
        }
        reciveMode = reciveMode_SYNHRO;
    }


    @Override
    public void sendMessageStopAuto() {
//        byte[] header = {
//                // заголовок
//                (byte)0xe6
//                ,(byte)0x19
//                ,(byte)0x55
//                ,(byte)0xaa
//        };
//        byte[] body = {
//                // код передачи
//                (byte)0x80
//        };
//        port.writeBytes(headBuffer, header.length);
//        // длина передачи
//        {
//            byte[] dl = new byte[1];
//            dl[0] = (byte) (body.length + (1 & 0x000000ff));
//            port.writeBytes(dl, dl.length);
//        }
//        // тело передачи
//        port.writeBytes(body, body.length);
//        // контрольная сумма
//        {
//            byte[] cs = new byte[1];
//            cs[0] = ControlSumma.crc8(body, body.length);
//            port.writeBytes(cs, cs.length);
//        }
    }
}
