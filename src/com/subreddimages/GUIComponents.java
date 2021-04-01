package com.subreddimages;

import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.MouseListener;

import javax.swing.ComboBoxModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GUIComponents {
	
	public static class Components{ //NEATENS UP THE CODE IN THE MAIN GUI SCRIPT
		public javax.swing.JFrame JFrame(String title, Font font, Boolean resizable, Color background, Integer closetype, LayoutManager layout, Integer[] bounds){
			javax.swing.JFrame jframe = new javax.swing.JFrame(title);
			jframe.setFont(font);
			jframe.setResizable(resizable);
			jframe.getContentPane().setBackground(background);
			jframe.getContentPane().setLayout(layout);
			jframe.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			jframe.setDefaultCloseOperation(closetype);
			return jframe;
		}
		public javax.swing.JButton JButton(String name, MouseListener mouseevent, String tooltiptext, Color foreground, Font font, Integer[] bounds) {
			javax.swing.JButton jbutton = new javax.swing.JButton(name);	
			jbutton.addMouseListener(mouseevent);
			jbutton.setToolTipText(tooltiptext);
			jbutton.setForeground(foreground);
			jbutton.setFont(font);
			jbutton.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);	
			return jbutton;
		}
		public javax.swing.JPanel JPanel(Border border, Color background, LayoutManager layout, Integer[] bounds){
			javax.swing.JPanel jpanel = new javax.swing.JPanel();
			jpanel.setBorder(border);
			jpanel.setBackground(background);
			jpanel.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			jpanel.setLayout(layout);
			return jpanel;
		}
		public javax.swing.JLabel JLabel(String text, String name, String tooltiptext, Font font, Color foreground, Integer[] bounds){
			javax.swing.JLabel jlabel = new javax.swing.JLabel(text);
			jlabel.setToolTipText(tooltiptext);
			jlabel.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			jlabel.setFont(font);
			jlabel.setForeground(foreground);
			jlabel.setName(name);
			return jlabel;
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public javax.swing.JComboBox JComboBox(String name, String tooltiptext, Color foreground, Color background, Font font, ComboBoxModel<?> model, Integer[] bounds){
			javax.swing.JComboBox jcombobox = new javax.swing.JComboBox();
			jcombobox.setName(name);
			jcombobox.setToolTipText(tooltiptext);
			jcombobox.setForeground(foreground);
			jcombobox.setBackground(background);
			jcombobox.setFont(font);
			jcombobox.setModel(model);
			jcombobox.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jcombobox;
		}
		public javax.swing.JCheckBox JCheckBox(String name, String tooltiptext, Integer[] bounds){
			javax.swing.JCheckBox jcheckbox = new javax.swing.JCheckBox();
			jcheckbox.setName(name);
			jcheckbox.setToolTipText(tooltiptext);
			jcheckbox.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jcheckbox;
		}
		public javax.swing.JSlider JSlider(String name, String tooltiptext, ChangeListener listener, Integer value, Integer min, Integer max, Integer[] bounds){
			javax.swing.JSlider jslider = new javax.swing.JSlider();
			jslider.setName(name);
			jslider.setToolTipText(tooltiptext);
			jslider.setMinimum(min);
			jslider.setMaximum(max);
			jslider.setValue(value);
			jslider.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			jslider.addChangeListener(listener);
			return jslider;	
		}
		public javax.swing.JRadioButton JRadioButton(String text, String name, Boolean selected, Color foreground, Font font, Integer[] bounds){
			javax.swing.JRadioButton jradiobutton = new javax.swing.JRadioButton(text);
			jradiobutton.setName(name);
			jradiobutton.setFont(font);
			jradiobutton.setForeground(foreground);
			jradiobutton.setSelected(selected);
			jradiobutton.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jradiobutton;
		}
		public javax.swing.JFileChooser JFileChooser(Font font, Color background, Integer selectionmode, FileNameExtensionFilter fex, Boolean acceptall){
			javax.swing.JFileChooser jfilechooser = new javax.swing.JFileChooser();
			jfilechooser.setFont(font);
			jfilechooser.setBackground(background);
			if(selectionmode != null) {
				jfilechooser.setFileSelectionMode(selectionmode);
			}
			jfilechooser.setFileFilter(fex);
			jfilechooser.setAcceptAllFileFilterUsed(acceptall);
			return jfilechooser;
		}
		public javax.swing.JTextField JTextField(Font font, Color foreground, Color background, Integer columns, Integer[] bounds){
			javax.swing.JTextField jtextfield = new javax.swing.JTextField();
			jtextfield.setColumns(columns);
			jtextfield.setFont(font);
			jtextfield.setForeground(foreground);
			jtextfield.setBackground(background);
			jtextfield.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jtextfield;
		}
		public javax.swing.JProgressBar JProgressBar(Integer value, Color background, Boolean enabled, Boolean indeterminate, Integer[] bounds){
			javax.swing.JProgressBar jprogressbar = new javax.swing.JProgressBar();
			jprogressbar.setValue(value);
			jprogressbar.setIndeterminate(indeterminate);
			jprogressbar.setBackground(background);
			jprogressbar.setEnabled(enabled);
			jprogressbar.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jprogressbar;
		}
		public javax.swing.JTextPane JTextPane(Font font, Color foreground, Color background, Boolean editable, String contenttype, Integer[] bounds){
			javax.swing.JTextPane jtextpane = new javax.swing.JTextPane();
			jtextpane.setFont(font);
			jtextpane.setForeground(foreground);
			jtextpane.setBackground(background);
			jtextpane.setEditable(editable);
			jtextpane.setContentType(contenttype);
			jtextpane.setBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
			return jtextpane;
		}
		public javax.swing.JScrollPane JScrollPane(javax.swing.JTextPane pane, Integer verticalscrollpolicy, Integer horizontalscrollpolicy){
			javax.swing.JScrollPane jscrollpane = new javax.swing.JScrollPane(pane);
			jscrollpane.setVerticalScrollBarPolicy(verticalscrollpolicy);
			jscrollpane.setHorizontalScrollBarPolicy(horizontalscrollpolicy);
			return jscrollpane;
		}
	}


}
