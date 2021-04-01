package com.subreddimages;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.UIManager;


public class MainGUI {

	/*
	 * Creates an instance of the Components class for easier use.
	 */
	private GUIComponents.Components cm = new GUIComponents.Components();
	/*
	 * Everything uses the same font.
	 */
	private Font font = new Font("Courier New", Font.BOLD, 13);
	/*
	 * All these components are used outside the scope of the function they are in.
	 */
	private JProgressBar progressBar;
	private JPanel sub_options;
	private JPanel sub_select;
	private JFrame frame;
	private String path_out;
	private String path_to_image;
	private JButton start;
	private JButton end;
	private ButtonGroup buttons = new ButtonGroup();
	private JTextField sub_name;
	private JLabel directory_label;
	/*
	 * Allows Python to run while the GUI stays responsive.
	 */
	private SwingWorker<?, ?> python = null;
	/*
	 * This mouse listener starts the process (when the button says 'start')
	 */
	MouseListener startML = new MouseAdapter() {
		public void mouseReleased(java.awt.event.MouseEvent evt) {
			Check();
		}
	};
	/*
	 * This mouse listener ends the process (when the button says 'cancel')
	 */
	MouseListener endML = new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
			try {
				CancelProcess(true);
			} catch (Exception ex) {}
		}
	};
	/*
	 * 'rt' is the virtual environment in which the Python process 'proc' lives.
	 */
	Runtime rt = null;
	Process proc = null;

	public static void main(String[] args) {
		FlatDarkLaf.install();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				startGUI();
			}
		});
	}

	public static void startGUI() {
		try { UIManager.setLookAndFeel(new FlatDarkLaf()); }
		catch(Exception e) {}
		_Logger.initialize(); // Initialises the Logger
		_Logger.Log( // Log the OS type. (Some errors may be different on different OS)
				String.format("OS: %s", System.getProperty("os.name")), Level.CONFIG);
		MainGUI main = new MainGUI();
		main.frame.setVisible(true);
		_Logger.Log("GUI initalized successfuly!", Level.INFO);
	}

	public MainGUI() {
		_Logger.Log("Initializing GUI", Level.INFO);
		initialize();
	}

	private void initialize() {	
		frame = cm.JFrame("Subreddimages", font, false, Color.DARK_GRAY, JFrame.EXIT_ON_CLOSE, null,
				new Integer[] { 100, 100, 428, 507 });
		
		/*
		 * Does stuff when the windows is closed.
		 */
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {

        		int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit?",
        					JOptionPane.YES_NO_OPTION);
        		if (result == JOptionPane.YES_OPTION) {
        			_Logger.Log("GUI Closed!", Level.INFO);
                    _Logger.Close();
                    e.getWindow().dispose();
        		}
        		else {
        			return;
        		}
            }
        });

		SubredditSelection(frame);
		SubredditOptions(frame);

		start = cm.JButton("Run!", startML, "Starts the process.", Color.BLACK, font,
				new Integer[] { 94, 439, 247, 29 });
		end = cm.JButton("Cancel!", endML, "Ends the process.", Color.BLACK, font,
				new Integer[] { 94, 439, 247, 29 });
		end.setVisible(false);

		frame.getContentPane().add(start);
		frame.getContentPane().add(end);
	}

	public void startPythonThread() {
		
		end.setVisible(true);
		start.setVisible(false);
		
		// Disable all the components
		for (Component component : Functions.getComponents(sub_select))
			component.setEnabled(false);
		for (Component component : Functions.getComponents(sub_options))
			component.setEnabled(false);
		
		start.setEnabled(true); // Re-enable the button (after being disabled).
		frame.repaint(); // Redraws GUI so you can see changes.
		start.setText("Cancel");
		Functions.infoBox("This could take a long time!", "Warning", JOptionPane.WARNING_MESSAGE);

		/* This is the process that allows the Python script to run, while also allowing
		 * the GUI to still be active otherwise, you wouldn't be able to cancel the
		 * Python script since the GUI would be frozen.
		 */

		python = new SwingWorker<Object, Object>() {

			/* 
			 * The thread runs in the background. Anything needed to run in the background goes in this function.
			 */

			protected Boolean doInBackground() {
				String command = new String();

				try {

					/* 
					 * This function is going to try and find the Python 3 location by running
					 * find_py.py. If it's not found it errors otherwise it uses the path in the
					 * main command
					 */

					String dir = System.getProperty("user.dir");
					String find_py = System.getProperty("os.name").startsWith("Windows") ? dir + "\\find_py.py"
							: dir + "/find_py.py";
					String main_py = System.getProperty("os.name").startsWith("Windows") ? dir + "\\main.py"
							: dir + "/main.py";
					
					File f = new File(find_py);
					File f1 = new File(main_py);
					if(!f.exists() || !f1.exists()) { 
						_Logger.Log("Path to python file(s) is not valid: "+find_py+" -- "+main_py, Level.SEVERE);
						Functions.infoBox("Python file(s) cannont be found!", "Error", JOptionPane.ERROR_MESSAGE);
						CancelProcess(false);
					}

					String[] get_path = new String[] { "python", find_py };

					List<List<String>> all_output = Functions.runCommand(get_path, false);
					List<String> output = all_output.get(0);

					if (output.get(0).contains("END")) {
						_Logger.Log("Python 3 version not found!", Level.SEVERE);
						Functions.infoBox("Error: Python 3 version not found! Download from: https://www.python.org/downloads/", "Error", JOptionPane.ERROR_MESSAGE);
						CancelProcess(false);
					}

					/*
					 * This is the command that is going to be run. It includes all the arguments
					 * the Python script needs to successfully run.
					 */

					command = new String(String.format("%s -u %s %s %s %s %s %s %s %s %s",
							output.get(0), //Python 3 path
							main_py, //Main Python File
							String.format("-max %s ", 
									((JSlider) Functions.getComponentByName("max_img_slider", sub_options)).getValue()),
							String.format("-sub %s ", 
									sub_name.getText()),
							String.format("-nsfw %s ", 
									Boolean.toString( ((JCheckBox) Functions.getComponentByName("nsfw_check", sub_options)).isSelected())),
							String.format("-near %s ", 
									(String) Functions.getSelectedButtonText(buttons)),
							String.format("-sort %s ", 
									"\""+((JComboBox<?>) Functions.getComponentByName("sort_by", sub_options)).getSelectedItem().toString().replace(' ', '-'))+"\"",
							String.format("-size %s ", 
									((JSlider) Functions.getComponentByName("img_section_size", sub_options)).getValue()),
							String.format("-in %s ", 
									"\""+path_to_image+"\""),
							String.format("-out %s ", "\""+path_out+"\"")));

				} catch (Exception e) {
					_Logger.fullLog(e);
					CancelProcess(false);
				}

				_Logger.Log(String.format("COMMAND: %s", String.join(", ", command)), Level.CONFIG);
				try {
					
					// This runs the main command to start the procces.
					rt = Runtime.getRuntime();
					proc = rt.exec(command);
					
					getOutput(proc); //Retrieve the output
					
				}
				
				catch (Exception e) {
					_Logger.fullLog(e);
					CancelProcess(false);
				}
				return null;
			}

			// This function runs when everything in doInBackground() is completed (or when
			// an uncaught error occurs). This function just ends the process.
			protected void done() {
				_Logger.Log("-----------------", Level.INFO);
				_Logger.Log("Python finished executing!", Level.INFO);
				CancelProcess(false);
			}
		};

		progressBar.setIndeterminate(true); //'Enable' the progress bar
		python.execute();

	}
	
	/*
	 * Gets the output from the command using a separate thread
	 */
	public void getOutput(Process process) {
        BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s = null; 
        _Logger.Log("-----------------", Level.INFO);
        try {
        	while((s = input.readLine()) != null){
        		if (s.contains("END")) {
        			_Logger.Log("Python: "+s, Level.SEVERE);
        		}
        		else {
        			_Logger.Log("Python: "+s, Level.INFO);
        		}
        	}

        	input.close();
        } catch (IOException e) {
            _Logger.fullLog(e);
            CancelProcess(false);
        }
	}

	/*
	 * This function is to cancel the Python process. If it was cancelled by mouse
	 * (true) there will be a warning, otherwise it will just continue with no
	 * warning (false). (Normally happens when an error occurs, and it needs to stop
	 * the process).
	 * 
	 */
	public void CancelProcess(boolean cancelledByMouse) {
		int result = 0;
		if (cancelledByMouse) {
			result = JOptionPane.showConfirmDialog(null, "Are you sure you want to cancel?", "Cancel?",
					JOptionPane.YES_NO_OPTION);
			_Logger.Log("Canceling process", Level.INFO);
		} else {
			result = JOptionPane.YES_OPTION;
		}
		if (result == JOptionPane.YES_OPTION) {
			python.cancel(true);
			proc.destroy(); 
			
			end.setVisible(false);
			start.setVisible(true);

			for (Component component : Functions.getComponents(sub_select))
				component.setEnabled(true);
			for (Component component : Functions.getComponents(sub_options))
				component.setEnabled(true);
			start.setText("Run!");
			progressBar.setIndeterminate(false);

			frame.repaint();
		}
	}

	/*
	 * Check() makes sure everything entered by the user is correct. It check
	 * for: 
	 * - Subreddit names having no non alphabetical character (numbers,
	 * symbols...) 
	 * - The path to the output directory exists (otherwise it uses the
	 * current directory) 
	 * - The path to the image exists. If all these are passed,
	 * then it can start Python.
	 */
	public void Check() {
		if (!sub_name.getText().equals("")) {
			Pattern reg = Pattern.compile("[^a-zA-Z]"); // Easy to use a regex to test.
			Matcher matcher = reg.matcher(sub_name.getText());
			if (matcher.find()) {
				_Logger.Log("Subreddit name must only use alphabetical characters!", Level.WARNING);
				Functions.infoBox("Subreddit name must only use alphabetical characters!", "Error",
						JOptionPane.ERROR_MESSAGE);
			} else {
				if (path_out == null || path_out.equals("")) {
					_Logger.Log("Output path is empty, using current directory!", Level.WARNING);
					path_out = System.getProperty("user.dir");
					directory_label.setText(path_out);
				} else {
					if (path_to_image == null || path_to_image.equals("")) {
						_Logger.Log("Image path is empty!", Level.WARNING);
						Functions.infoBox("Please select an image to create!", "Error",
								JOptionPane.ERROR_MESSAGE);
						path_out = System.getProperty("user.dir");
						directory_label.setText(path_out);
					} else {
						_Logger.Log("Passed checks, can start", Level.INFO);
						startPythonThread();
					}
				}
			}
		} else if (sub_name.getText().equals("")) {
			_Logger.Log("Subreddit name cannot be empty!", Level.WARNING);
			Functions.infoBox("Subreddit name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
		}

	}

	public void SubredditOptions(JFrame frame) {
		/*
		 * This JPanel has all the extra options.
		 */
		sub_options = cm.JPanel(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null), Color.DARK_GRAY, null,
				new Integer[] { 6, 200, 415, 235 });
		
		frame.getContentPane().add(sub_options);
		/*
		 * Label that shows how the subreddit post should be sorted.
		 */
		JLabel sort_by_label = cm.JLabel("Sort by:", "sort_by_label", "The sort of the subreddit.", font, Color.WHITE,
				new Integer[] { 6, 12, 64, 16 });
		sub_options.add(sort_by_label);
		/*
		 * Drop down menu with all the sort options.
		 */
		JComboBox<?> sort_by = cm.JComboBox("sort_by", "", Color.BLACK, Color.WHITE, font,
				new DefaultComboBoxModel<Object>(
						new String[] { "Past Day", "Past Week", "Past Month", "Past Year", "All time" }),
				new Integer[] { 82, 8, 160, 27 });
		sub_options.add(sort_by);
		/*
		 * Label for NSFW check box
		 */
		JLabel nsfw_label = cm.JLabel("NSFW / L:", "nsfw_label", "Whether NSFW/L posts should be included.", font,
				Color.WHITE, new Integer[] { 6, 65, 74, 16 });
		sub_options.add(nsfw_label);
		/*
		 * Check box that determined whether NSFW is allowed.
		 */
		JCheckBox nsfw_check = cm.JCheckBox("nsfw_check", "", new Integer[] { 92, 53, 118, 39 });
		sub_options.add(nsfw_check);
		/*
		 * Label for crop size slider.
		 */
		JLabel image_sectionS_label = cm.JLabel("Image section size:", "image_sectionS_label",
				"The size of the section of the image the program will take.", font, Color.WHITE,
				new Integer[] { 6, 146, 160, 16 });
		sub_options.add(image_sectionS_label);
		/*
		 * Label to show what value on the slider is selected.
		 */
		JLabel img_sectionS_value = cm.JLabel("", "img_sectionS_value", "", font, Color.WHITE,
				new Integer[] { 352, 146, 52, 16 });
		sub_options.add(img_sectionS_value);
		/*
		 * Slider that determines crop size.
		 */
		JSlider img_section_size = cm.JSlider("img_section_size", "", null, 100, 100, 400,
				new Integer[] { 161, 136, 190, 39 });
		img_section_size.setMajorTickSpacing(2); //Changes in steps of 2
		img_section_size.setSnapToTicks(true);
		ChangeListener change_size = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				img_sectionS_value.setText(Integer.toString(img_section_size.getValue()));
			}
		};
		img_section_size.addChangeListener(change_size);
		img_sectionS_value.setText(Integer.toString(img_section_size.getValue()));
		sub_options.add(img_section_size);
		/*
		 * Label for maximum images slider.
		 */
		JLabel max_img_value = cm.JLabel("", "max_img_value", "", font, Color.WHITE,
				new Integer[] { 317, 196, 91, 17 });
		sub_options.add(max_img_value);
		/*
		 * Label that shows the value on the slider.
		 */
		JLabel max_images = cm.JLabel("Maximum images:", "max_images",
				"Maximum number of images allowed to be downloaded.", font, Color.WHITE,
				new Integer[] { 6, 196, 128, 16 });
		sub_options.add(max_images);
		/*
		 * Slider which determines the max amount of images that can be retrieved.
		 */
		JSlider max_img_slider = cm.JSlider("max_img_slider", "", null, 3000, 500, 10000,
				new Integer[] { 127, 184, 190, 45 });
		ChangeListener change_max = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				max_img_value.setText(Integer.toString(max_img_slider.getValue()));
			}
		};
		max_img_slider.addChangeListener(change_max);
		max_img_value.setText(Integer.toString(max_img_slider.getValue()));
		sub_options.add(max_img_slider);
		/*
		 * Label to show how images are picked
		 */
		JLabel nearest = cm.JLabel("Nearest by:", "nearest_by",
				"Whether it should pick the image depending on it's brightntess or colour.", font, Color.WHITE,
				new Integer[] { 6, 107, 103, 16 });
		sub_options.add(nearest);
		/*
		 * Radio button for colour
		 */
		JRadioButton nearest_colour = cm.JRadioButton("Colour", "colour", true, Color.WHITE, font,
				new Integer[] { 102, 104, 91, 23 });
		sub_options.add(nearest_colour);
		/*
		 * Radio button for brightness
		 */
		JRadioButton nearest_bright = cm.JRadioButton("Brightness", "brightness", false, Color.WHITE, font,
				new Integer[] { 192, 104, 141, 23 });
		sub_options.add(nearest_bright);
		/*
		 * Add both to a button group so only one can be selected at a time.
		 */
		buttons.add(nearest_colour);
		buttons.add(nearest_bright);

	}

	public void SubredditSelection(JFrame frame) {
		/*
		 * File choosers for selecting input image and output directory.
		 */
		final JFileChooser fc_out = cm.JFileChooser(font, Color.DARK_GRAY, JFileChooser.DIRECTORIES_ONLY, null, false);
		final JFileChooser fc_img = cm.JFileChooser(font, Color.DARK_GRAY, null,
				new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg", "gif"), false);
		/*
		 * JPanel for housing a few things.
		 */
		sub_select = cm.JPanel(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null), Color.DARK_GRAY, null,
				new Integer[] { 6, 6, 415, 182 });
		frame.getContentPane().add(sub_select);
		/*
		 * Text box where subreddit name goes.
		 */
		sub_name = cm.JTextField(font, Color.WHITE, Color.DARK_GRAY, 10, new Integer[] { 90, 18, 319, 33 });
		sub_select.add(sub_name);
		/*
		 * Label for text box
		 */
		JLabel sub_name_box_label = cm.JLabel("Subreddit:", "subreddit", "The name of the subreddit.", font,
				Color.WHITE, new Integer[] { 6, 26, 101, 16 });
		sub_select.add(sub_name_box_label);
		/*
		 * Progress bar to indicate program is running (and GUI is responding)
		 */
		progressBar = cm.JProgressBar(0, Color.WHITE, true, false, new Integer[] { 6, 465, 415, 20 });
		frame.getContentPane().add(progressBar);
		/*
		 * Label for output directory.
		 */
		JLabel output_directory = cm.JLabel("Output Directory:", "out_dir", "Output directory for the image.", font,
				Color.WHITE, new Integer[] { 6, 82, 152, 16 });
		sub_select.add(output_directory);
		/*
		 * Label to show the currently selected directory
		 */
		directory_label = cm.JLabel("", "selected_dir", "", font, Color.WHITE, new Integer[] { 152, 110, 257, 16 });
		sub_select.add(directory_label);
		/*
		 * Button to open the file chooser to select directory.
		 */
		JButton file_chooser = cm.JButton("Choose Folder", null, "", null, font, new Integer[] { 152, 77, 136, 29 });
		MouseAdapter filechoose = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int returnVal = fc_out.showOpenDialog(file_chooser);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					directory_label.setText(fc_out.getSelectedFile().getAbsolutePath());
					path_out = fc_out.getSelectedFile().getAbsolutePath();
				}
			}
		};
		file_chooser.addMouseListener(filechoose);
		sub_select.add(file_chooser);
		/*
		 * Label to show what image is currently selected.
		 */
		JLabel image_dir_label = cm.JLabel("", "im_dir_lbl", "", font, Color.WHITE,
				new Integer[] { 152, 160, 257, 16 });
		sub_select.add(image_dir_label);
		/*
		 * Label for image directory
		 */
		JLabel to_make_label = cm.JLabel("Image to create:", "to_make_lbl", "The image you want created.", font,
				Color.WHITE, new Integer[] { 6, 137, 136, 16 });
		sub_select.add(to_make_label);
		/*
		 * Button that opens the file chooser to choose image.
		 */
		JButton image_chooser = cm.JButton("Choose Image", null, "", null, font, new Integer[] { 142, 132, 136, 29 });
		MouseAdapter imagechoose = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int returnVal = fc_img.showOpenDialog(image_chooser);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					image_dir_label.setText(fc_img.getSelectedFile().getAbsolutePath());
					path_to_image = fc_img.getSelectedFile().getAbsolutePath();
				}
			}
		};
		image_chooser.addMouseListener(imagechoose);

		sub_select.add(image_chooser);
	}
}