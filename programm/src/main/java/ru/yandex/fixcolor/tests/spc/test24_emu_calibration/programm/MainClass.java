package ru.yandex.fixcolor.tests.spc.test24_emu_calibration.programm;

import ru.yandex.fixcolor.library.swing.components.modifed.MLabel;
import ru.yandex.fixcolor.library.swing.utils.CreateComponents;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainClass {
    public static void main(String[] args) {
        System.out.println("start");
        new MainClass().start();
    }
    private void start() {
        init_components();
    }
    private JFrame frame;
    // ---
    Object ves_lock = new Object();
    private JTextField ves_multiply_text;
    private JTextField ves_offset_text;
    private JTextField ves_render_text;
    private JSlider ves_slider;
    private JLabel ves_slider_label;
    double ves_multiply = 1.0;
    double ves_offset = 0;
    int ves_adc;
    // ---
    Object dist_lock = new Object();
    private JTextField dist_multiply_text;
    private JTextField dist_offset_text;
    private JTextField dist_render_text;
    private JSlider dist_slider;
    private JLabel dist_slider_label;
    double dist_multiply = 1.0;
    double dist_offset = 0;
    int dist_adc;
    // ---
    private void init_components() {
        frame = CreateComponents.getFrame("calibration", 800, 600,
                false, null, null);
        //
        init_components_ves(frame);
        init_components_dist(frame);
        //
        //
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void init_components_ves(Container parent) {
        ves_slider = new JSlider( JSlider.HORIZONTAL, 0, 1023, 512);
        ves_slider.setBounds(25, 100, 750, 50);
        ves_slider.setPaintLabels(true);
        ves_slider.setPaintTicks(true);
        ves_slider.setMajorTickSpacing(50);
        ves_slider.setMinorTickSpacing(10);
        ves_slider.addChangeListener(this::renderVes);
        ves_slider_label = CreateComponents.getLabel(parent, "",
                new Font("Times New Roman", Font.PLAIN, 16),
                150, 85, true, true, MLabel.CENTER);
        ves_multiply_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 10, 100, 30,
                null, this::cVes, true, true);
        ves_offset_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 55, 100, 30,
                null, this::cVes, true, true);
        ves_render_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 32),
                300, 15, 150, 60,
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
        ves_slider_label.setLocation((int) (35 + ((double) 715 / 1023) * ves_adc), ves_slider_label.getY());
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
        dist_slider.setBounds(25, 300, 750, 50);
        dist_slider.setPaintLabels(true);
        dist_slider.setPaintTicks(true);
        dist_slider.setMajorTickSpacing(50);
        dist_slider.setMinorTickSpacing(10);
        dist_slider.addChangeListener(this::renderDist);
        dist_slider_label = CreateComponents.getLabel(parent, "",
                new Font("Times New Roman", Font.PLAIN, 16),
                150, 285, true, true, MLabel.CENTER);
        dist_multiply_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 210, 100, 30,
                null, this::cDist, true, true);
        dist_offset_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 18),
                150, 255, 100, 30,
                null, this::cDist, true, true);
        dist_render_text = CreateComponents.getTextField(CreateComponents.TEXTFIELD,
                new Font("Times New Roman", Font.PLAIN, 32),
                300, 215, 150, 60,
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
        dist_slider_label.setLocation((int) (35 + ((double) 715 / 1023) * dist_adc), dist_slider_label.getY());
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
}