package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232;

import com.fazecast.jSerialComm.SerialPort;
import ru.yandex.fixcolor.library.controlsumma.ControlSumma;

class CommPortClass implements CommPort {

    private SerialPort port = null;
    private Thread threadRS = null;
    private CallBack callBack = null;

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

    private int onCycle;

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
        onCycle = -1;

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
    final private int headBufferLenght  = 5;
    final private int timeOutLenght     = 5;
    // ---------------------
    private int timeOutSynhro = 1;
    private boolean flagHead = true;
    private byte[]  headBuffer = new byte[headBufferLenght];
    private int lenghtRecive;
    private int lenghtReciveSumm;
    private byte crc;

    private void runner() {
        // flush
        int num = 1;
        byte[] bytes = new byte[1000];

        while (num > 0) {
            num = port.readBytes(bytes, bytes.length);
        }

        onCycle = 1;
        while (onCycle >= 0) {
            if (onCycle > 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (timeOutSynhro > 1)    timeOutSynhro--;
                if (timeOutSynhro == 1) {
                    timeOutSynhro = 0;
                    flagHead = true;
                }
            }

            if (flagHead) {
                for (int i = 0; i < headBufferLenght - 1; i++) {
                    headBuffer[i] = headBuffer[i + 1];
                }

                try {
                    num = port.readBytes(headBuffer, 1, headBufferLenght - 1);
                } catch (Exception e) {
                    e.printStackTrace();
                    e = null;
                }

                if (num < 0) {
                    onCycle = -1;
                    continue;
                }

                if (num == 0) {
                    if (onCycle < 0) continue;
                    onCycle = 1;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (onCycle < 0) continue;
                onCycle = 0;

                if (headBuffer[0] != (byte)0xe6)    continue;
                if (headBuffer[1] != (byte)0x19)    continue;
                if (headBuffer[2] != (byte)0x55)    continue;
                if (headBuffer[3] != (byte)0xaa)    continue;

                flagHead = true;
                timeOutSynhro = timeOutLenght;
                lenghtRecive = headBuffer[4] & 0x000000ff;
                lenghtReciveSumm = 0;
            }
            else {
                continue;
            }

            while (lenghtReciveSumm < lenghtRecive) {
                num = port.readBytes(bytes, lenghtRecive - lenghtReciveSumm, lenghtReciveSumm);

                if (num < 0) {
                    onCycle = -1;
                    continue;
                }

                if (num == 0) {
                    if (onCycle < 0) continue;
                    onCycle = 1;
                    continue;
                }

                if (onCycle < 0) continue;
                onCycle = 0;
                lenghtReciveSumm += num;
            }

            if (lenghtRecive > 1) {
                crc = ControlSumma.crc8(bytes, lenghtRecive - 1);
                if (crc == bytes[lenghtRecive - 1]) {
                    callBack.reciveRsPush(bytes, lenghtRecive - 1);
                }
            }

            flagHead = true;
        }
    }

    @Override
    public void sendMessageStopAuto() {
        byte[] header = {
                // заголовок
                (byte)0xe6
                ,(byte)0x19
                ,(byte)0x55
                ,(byte)0xaa
        };
        byte[] body = {
                // код передачи
                (byte)0x80
        };
        port.writeBytes(headBuffer, header.length);
        // длина передачи
        {
            byte[] dl = new byte[1];
            dl[0] = (byte) (body.length + (1 & 0x000000ff));
            port.writeBytes(dl, dl.length);
        }
        // тело передачи
        port.writeBytes(body, body.length);
        // контрольная сумма
        {
            byte[] cs = new byte[1];
            cs[0] = ControlSumma.crc8(body, body.length);
            port.writeBytes(cs, cs.length);
        }
    }
}
