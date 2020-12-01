package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

import ru.yandex.fixcolor.library.swing.components.modifed.MLabel;
import ru.yandex.fixcolor.library.swing.utils.CreateComponents;
import ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232.BAUD;
import ru.yandex.fixcolor.tests.spc.test24_emu_calibration.rs232.CommPort;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

public class MainClass {
    public static MainClass main;
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        System.out.println("start");
        new MainClass().start();
    }
    private void start() {
        main = this;
        flagCalibration = false;
        timer = new Timer(this::timer_execute);
        timerMode = new TimerCount(this::timerMode_execute, 100, 20, "timer mode");
        timerCalibration = new TimerCount(this::timerCalibration_execute, 20, 10, "timer calibration");
        //---
        init_components();
        commPort = CommPort.init();
        CommPort.PortStat portStat = commPort.open(this::reciveFromRs, "com6", BAUD.baud57600);
        //---
        timerMode.pusk();
        work = new Work(commPort);
        work.init();
        timer.start();
        commPort.ReciveStart();
    }
    private JFrame frame;
    // ---
    Object ves_lock = new Object();
    private JTextField ves_multiply_text;
    private JTextField ves_offset_text;
    private JTextField ves_render_text;
    private JSlider ves_slider;
    private JLabel ves_slider_label;
    double ves_multiply = 0.489;
    double ves_offset = 0;
    int ves_adc;
    // ---
    Object dist_lock = new Object();
    private JTextField dist_multiply_text;
    private JTextField dist_offset_text;
    private JTextField dist_render_text;
    private JSlider dist_slider;
    private JLabel dist_slider_label;
    double dist_multiply = 0.782;
    double dist_offset = 0;
    int dist_adc;
    // ---
    private JPanel panelSwich;
    private ButtonGroup emu_buttonGroup;
    private JRadioButton emu_buttonStop;
    private JRadioButton emu_buttonStartOne;
    private JRadioButton emu_buttonStartMulti;
    // ---
    public JTextField statusText;
    private CommPort commPort;
    // ---
    private JTextField distance_begin_name;
    public JTextField distance_begin_time;
    public JTextField distance_begin_distance;
    // ---
    private JTextField distance_start_name;
    public JTextField distance_start_time;
    public JTextField distance_start_distance;
    // ---
    private JTextField distance_shelf_name;
    public JTextField distance_shelf_time;
    public JTextField distance_shelf_distance;
    // ---
    private JTextField distance_back_name;
    public JTextField distance_back_time;
    public JTextField distance_back_distance;
    // ---
    private JTextField distance_stop_name;
    public JTextField distance_stop_time;
    public JTextField distance_stop_distance;
    // ---
    private JTextField distance_delay_name;
    public JTextField distance_delay_time;
    public JTextField distance_delay_distance;
    // ---
    private Timer timer;
    private Work work;
    // ---
    private TimerCount timerMode;
    private boolean flagCalibration;
    private TimerCount timerCalibration;
    // ---------------------
    private void init_components() {
        frame = CreateComponents.getFrame("calibration", 1000, 600,
                false, null, null);
        //
        init_components_ves(frame);
        init_components_dist(frame);
        init_components_emu(frame);
        statusText = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                200, 390, 120, 30,
                null, null, true, true, false);
        frame.add(statusText);
        init_components_dist_uk(frame);
        //
        //
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new FrameStop());
    }

    private class FrameStop extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            super.windowClosing(e);
            commPort.ReciveStop();
            commPort.close();
            timer.close();
            //work.close();
        }
    }

    private void init_components_ves(Container parent) {
        ves_slider = new JSlider( JSlider.HORIZONTAL, 0, 1023, 512);
        ves_slider.setBounds(25, 300, 950, 50);
        ves_slider.setPaintLabels(true);
        ves_slider.setPaintTicks(true);
        ves_slider.setMajorTickSpacing(50);
        ves_slider.setMinorTickSpacing(10);
        ves_slider.addChangeListener(this::renderVes);
        ves_slider_label = CreateComponents.getLabel(parent, "",
                new Font("Times New Roman", Font.PLAIN, 16),
                150, 285, true, true, MLabel.CENTER);
        ves_multiply_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 210, 100, 30,
                null, this::cVes, true, true);
        ves_offset_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 255, 100, 30,
                null, this::cVes, true, true);
        ves_render_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 32),
                300, 215, 150, 60,
                null, null, true, true, false);
        parent.add(ves_slider);
        parent.add(ves_slider_label);
        parent.add(ves_multiply_text);
        parent.add(ves_offset_text);
        parent.add(ves_render_text);
        ves_slider.setValue(0);
        ves_multiply_text.setText(String.valueOf(ves_multiply));
        ves_offset_text.setText(String.valueOf(ves_offset));
    }
    private void cVes(ActionEvent actionEvent) {
        renderVes(null);
    }
    private void renderVes(ChangeEvent e) {
        synchronized (ves_lock) {
            ves_adc = ves_slider.getValue();
        }
        ves_slider_label.setLocation((int) (35 + ((double) (ves_slider.getWidth() - 35) / 1023) * ves_adc), ves_slider_label.getY());
        ves_slider_label.setText(String.valueOf(ves_adc));
        try {
            ves_multiply = Double.parseDouble(ves_multiply_text.getText());
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        try {
            ves_offset = Double.parseDouble(ves_offset_text.getText());
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        double zn = ves_adc * ves_multiply + ves_offset;
        ves_render_text.setText(String.format("%8.3f",zn));
    }
    private void init_components_dist(Container parent) {
        dist_slider = new JSlider( JSlider.HORIZONTAL, 0, 1023, 512);
        dist_slider.setBounds(25, 100, 950, 50);
        dist_slider.setPaintLabels(true);
        dist_slider.setPaintTicks(true);
        dist_slider.setMajorTickSpacing(50);
        dist_slider.setMinorTickSpacing(10);
        dist_slider.addChangeListener(this::renderDist);
        dist_slider_label = CreateComponents.getLabel(parent, "",
                new Font("Times New Roman", Font.PLAIN, 16),
                150, 85, true, true, MLabel.CENTER);
        dist_multiply_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 10, 100, 30,
                null, this::cDist, true, true);
        dist_offset_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 55, 100, 30,
                null, this::cDist, true, true);
        dist_render_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 32),
                300, 15, 150, 60,
                null, null, true, true, false);
        parent.add(dist_slider);
        parent.add(dist_slider_label);
        parent.add(dist_multiply_text);
        parent.add(dist_offset_text);
        parent.add(dist_render_text);
        dist_slider.setValue(0);
        dist_multiply_text.setText(String.valueOf(dist_multiply));
        dist_offset_text.setText(String.valueOf(dist_offset));
    }
    private void cDist(ActionEvent actionEvent) {
        renderDist(null);
    }
    private void renderDist(ChangeEvent e) {
        synchronized (dist_lock) {
            dist_adc = dist_slider.getValue();
        }
        dist_slider_label.setLocation((int) (35 + ((double) (dist_slider.getWidth() - 35) / 1023) * dist_adc), dist_slider_label.getY());
        dist_slider_label.setText(String.valueOf(dist_adc));
        try {
            dist_multiply = Double.parseDouble(dist_multiply_text.getText());
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        try {
            dist_offset = Double.parseDouble(dist_offset_text.getText());
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        double zn = dist_adc * dist_multiply + dist_offset;
        dist_render_text.setText(String.format("%8.3f",zn));
    }
    private void init_components_emu(Container parent) {
        panelSwich = new JPanel(new GridLayout(0, 1, 0, 5));
        panelSwich.setBounds(10, 380, 180, 120);
        panelSwich.setBorder(BorderFactory.createTitledBorder("переключатель на пульту"));
        emu_buttonGroup = new ButtonGroup();
        emu_buttonStop = new JRadioButton("Стоп");
        emu_buttonStartOne = new JRadioButton("Однократно");
        emu_buttonStartMulti = new JRadioButton("Многократно");
        // --
        emu_buttonGroup.add(emu_buttonStop);
        emu_buttonGroup.add(emu_buttonStartOne);
        emu_buttonGroup.add(emu_buttonStartMulti);
        emu_buttonStop.setSelected(true);
        panelSwich.add(emu_buttonStop);
        panelSwich.add(emu_buttonStartOne);
        panelSwich.add(emu_buttonStartMulti);
        // --
        parent.add(panelSwich);
        emu_buttonStop.addItemListener(this::callButtonStop);
        emu_buttonStartOne.addItemListener(this::callButtonOne);
        emu_buttonStartMulti.addItemListener(this::callBittonMulti);
    }

    private void init_components_dist_uk(Container parent) {
        {
            distance_begin_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    330, 390, 80, 30, null, null, true, true, false);
            distance_begin_name.setText("начало");
            parent.add(distance_begin_name);
            distance_begin_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    330, 430, 80, 30, null, null, true, true, true);
            distance_begin_time.setText("200");
            parent.add(distance_begin_time);
            distance_begin_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    330, 470, 80, 30, null, null , true, true, true);
            distance_begin_distance.setText("900");
            parent.add(distance_begin_distance);
        } // begin
        {
            distance_start_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    430, 390, 80, 30, null, null, true, true, false);
            distance_start_name.setText("старт");
            parent.add(distance_start_name);
            distance_start_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    430, 430, 80, 30, null, null, true, true, true);
            distance_start_time.setText("250");
            parent.add(distance_start_time);
            distance_start_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    430, 470, 80, 30, null, e ->distance_shelf_distance.setText(distance_start_distance.getText()) , true, true, true);
            distance_start_distance.setText("300");
            parent.add(distance_start_distance);
        } // start
        {
            distance_shelf_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    530, 390, 80, 30, null, null, true, true, false);
            distance_shelf_name.setText("полка");
            parent.add(distance_shelf_name);
            distance_shelf_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    530, 430, 80, 30, null, null, true, true, true);
            distance_shelf_time.setText("2500");
            parent.add(distance_shelf_time);
            distance_shelf_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    530, 470, 80, 30, null, null, true, true, false);
            distance_shelf_distance.setText(distance_start_distance.getText());
            parent.add(distance_shelf_distance);
        } // shelf
        {
            distance_back_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    630, 390, 80, 30, null, null, true, true, false);
            distance_back_name.setText("назад");
            parent.add(distance_back_name);
            distance_back_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    630, 430, 80, 30, null, null, true, true, true);
            distance_back_time.setText("300");
            parent.add(distance_back_time);
            distance_back_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    630, 470, 80, 30, null, null, true, true, true);
            distance_back_distance.setText("898");
            parent.add(distance_back_distance);
        } // back
        {
            distance_stop_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    730, 390, 80, 30, null, null, true, true, false);
            distance_stop_name.setText("стоп");
            parent.add(distance_stop_name);
            distance_stop_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    730, 430, 80, 30, null, null, true, true, true);
            distance_stop_time.setText("50");
            parent.add(distance_stop_time);
            distance_stop_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    730, 470, 80, 30, null, e -> distance_delay_distance.setText(distance_stop_distance.getText()), true, true, true);
            distance_stop_distance.setText("900");
            parent.add(distance_stop_distance);
        } // stop
        {
            distance_delay_name = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    830, 390, 80, 30, null, null, true, true, false);
            distance_delay_name.setText("пауза");
            parent.add(distance_delay_name);
            distance_delay_time = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    830, 430, 80, 30, null, null, true, true, true);
            distance_delay_time.setText("3000");
            parent.add(distance_delay_time);
            distance_delay_distance = CreateComponents.getTextField(CreateComponents.TEXTFIELD, new Font("Times New Roman", Font.PLAIN, 14),
                    830, 470, 80, 30, null, null, true, true, false);
            distance_delay_distance.setText(distance_stop_distance.getText());
            parent.add(distance_delay_distance);
        } // delay
    }

    private void callButtonStop(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.DESELECTED) return;
        work.switchStop();
    }

    private void callButtonOne(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.DESELECTED) return;
        work.switchOne();
    }

    private void callBittonMulti(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.DESELECTED) return;
        work.switchMulti();
    }


    private void reciveFromRs(byte[] bytes, int lenght) {
        if ((bytes[0] & 0xff) == 0x81) {
            if (timerMode != null) {
                timerMode.updateCount();
                timerCalibration.updateCount();
            }
        }
    }


    private void timer_execute() {
        timerMode.timer_execute();
        timerCalibration.timer_execute();
        if (flagCalibration) return;
        work.timer_execute();
    }

    private void timerMode_execute(boolean flag) {
        if (flag && !flagCalibration) {
            statusText.setText("Калибровка");
            System.out.println("start calib");
            work.resetMode();
            if (timerCalibration != null) {
                timerCalibration.updateCount();
                timerCalibration.pusk();
            }
        }
        if (!flag && flagCalibration) {
            statusText.setText("");
            if (timerCalibration != null) timerCalibration.stop();
        }
        flagCalibration = flag;
    }
    private void timerCalibration_execute(boolean flag) {
        if (timerCalibration != null) {
            try {
                if (flag) {
                    timerCalibration.updateCount();
                    commPort.sendDataMeasured((byte) TypePack.CALIBR_DATA, 0xffffffff, dist_adc, ves_adc);
                }
            } catch (Exception exception) {
            }
        }
    }
}
