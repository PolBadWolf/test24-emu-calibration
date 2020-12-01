package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232;

import com.fazecast.jSerialComm.SerialPort;
import ru.yandex.fixcolor.library.controlsumma.ControlSumma;
import ru.yandex.fixcolor.library.converterdigit.ConvertDigit;

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
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 1, 0);

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
        while (port.readBytes(reciveBody_Buffer, reciveBody_Buffer.length) > 0);
        recive_TimeOut = 0;
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
    private int recive_TimeOut;
    private static final int recive_TimeOutSet = 5;
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
                if (recive_num == 0) Thread.sleep(1);
                if (recive_TimeOut == 1) reciveMode = reciveMode_SYNHRO;
                if (recive_TimeOut > 0) recive_TimeOut--;
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
        if ((reciveHeader[0] & 0xff) != 0xe6) return;
        if ((reciveHeader[1] & 0xff) != 0x19) return;
        if ((reciveHeader[2] & 0xff) != 0x55) return;
        if ((reciveHeader[3] & 0xff) != 0xaa) return;
        reciveMode = reciveMode_LENGHT;
        recive_TimeOut = recive_TimeOutSet;
    }
    private void recive_lenght() throws Exception {
        recive_num = port.readBytes(reciveHeader_in, 1);
        if (recive_num == 0) return;
        reciveBody_lenght = reciveHeader_in[0] & 0xff;
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
                callBack.reciveRsPush(reciveBody_Buffer, reciveBody_lenght - 1);
            }
        }
        reciveMode = reciveMode_SYNHRO;
    }
    // ************************************************************************
    private static final byte[] header = {
            (byte)0xe6,
            (byte)0x19,
            (byte)0x55,
            (byte)0xaa
    };
    private void send_header() throws Exception {
        int l = port.writeBytes(header, header.length);
        if (l < 1) throw new Exception("ошибка отправки по comm port");
    }
    private byte[] send_lenghtVar = new byte[1];
    private void send_lenght(byte[] body) throws Exception {
        send_lenghtVar[0] = (byte) ((body.length + 1) & 0xff);
        int l = port.writeBytes(send_lenghtVar, 1);
        if (l < 1) throw new Exception("ошибка отправки по comm port");
    }
    // ---------------------
    private final byte[] sendDataMeasuredBody = new byte[9];
    @Override
    public void sendDataMeasured(byte sendCode, long tik, int distance, int weight) throws Exception {
        // code (1)
        sendDataMeasuredBody[0] = sendCode;
        // tik (4)
        ConvertDigit.int2bytes(tik, sendDataMeasuredBody, 1);
        // distance (2)
        ConvertDigit.int2bytes(distance, sendDataMeasuredBody, 5, 2);
        // weight (2)
        ConvertDigit.int2bytes(weight, sendDataMeasuredBody, 7, 2);
        send_header();
        send_lenght(sendDataMeasuredBody);
        port.writeBytes(sendDataMeasuredBody, sendDataMeasuredBody.length);
        // контрольная сумма
        byte[] cs = new byte[1];
        cs[0] = ControlSumma.crc8(sendDataMeasuredBody, sendDataMeasuredBody.length);
        int l = port.writeBytes(cs, cs.length);
        if (l < cs.length) throw new Exception("ошибка отправки по comm port");
    }
    private final byte[] sendDataStatusBody = new byte[5];
    @Override
    public void sendStatus(int code, int tik) throws Exception {
        sendDataStatusBody[0] = (byte) code;
        // tik (4)
        ConvertDigit.int2bytes(tik, sendDataStatusBody, 1);
        send_header();
        send_lenght(sendDataStatusBody);
        port.writeBytes(sendDataStatusBody, sendDataStatusBody.length);
        // контрольная сумма
        byte[] cs = new byte[1];
        cs[0] = ControlSumma.crc8(sendDataStatusBody, sendDataStatusBody.length);
        port.writeBytes(cs, cs.length);
    }
    private final byte[] sendDataWeightBody = new byte[7];

    @Override
    public void sendDataWeight(byte code, long tik, int weight) throws Exception {
        sendDataWeightBody[0] = (byte) code;
        // tik (4)
        ConvertDigit.int2bytes(tik, sendDataWeightBody, 1);
        // weight
        ConvertDigit.int2bytes(weight, sendDataWeightBody, 5, 2);
        send_header();
        send_lenght(sendDataWeightBody);
        port.writeBytes(sendDataWeightBody, sendDataWeightBody.length);
        // контрольная сумма
        byte[] cs = new byte[1];
        cs[0] = ControlSumma.crc8(sendDataWeightBody, sendDataWeightBody.length);
        port.writeBytes(cs, cs.length);
    }
}
