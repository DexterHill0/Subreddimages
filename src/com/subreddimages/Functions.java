package com.subreddimages;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;

public class Functions {
	/*
	 * Displays a dialog of any type.
	 */
	static void infoBox(String infoMessage, String titleBar, Integer typeOfMessage)
    {
		if(typeOfMessage != JOptionPane.ERROR_MESSAGE) {
			JOptionPane.showMessageDialog(null, infoMessage, titleBar, typeOfMessage);
		}
		else {
			JOptionPane.showMessageDialog(null, infoMessage + "\n Please see the log for more info.", titleBar, typeOfMessage);
		}
    }
	/*
	 * Will return an array full of all the components in a container.
	 */
	static Component[] getComponents(Component container) {
        ArrayList<Component> list = null;

        try {
            list = new ArrayList<Component>(Arrays.asList(
                  ((Container) container).getComponents()));
            for (int index = 0; index < list.size(); index++) {
                for (Component currentComponent : getComponents(list.get(index))) {
                    list.add(currentComponent);
                }
            }
        } catch (ClassCastException e) {
            list = new ArrayList<Component>();
        }

        return list.toArray(new Component[list.size()]);
        }
	
	/*
	 * Creates a component hash map and uses that to get the name the component by its name.
	 */
	public static Component getComponentByName(String name, Container pane) {
		HashMap<String, Component> componentMap = new HashMap<String, Component>();
        Component[] components = pane.getComponents();
        for(Component comp : components) {
        	componentMap.put(comp.getName(), comp);
        }
        
        if (componentMap.containsKey(name)) {
                return (Component) componentMap.get(name);
        }
        else return null;
	}
	/*
	 * Get the text of the selected button within a button group.
	 */
	public static String getSelectedButtonText(ButtonGroup buttonGroup) { 
        for (Enumeration<AbstractButton> buttons = buttonGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return button.getText();
            }
        }

        return null;
	}

	/*
	* Function for running terminal commands and getting output (and errors).
	* Return a 2D array with the main output and errors in their own arrays
	*
	* This function is actually only used once since I realised I needed "live" processing of the output text otherwise nothing will be written to the log file until it's finished executing.
	*/
	public static List<List<String>> runCommand(String[] command, Boolean giveErrors){
		List<List<String>> output = new ArrayList<List<String>>(); //An array of an array of strings
		List<String> temp = new ArrayList<String>(); //Array of strings

		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			String s = null;
			while ((s = stdInput.readLine()) != null) {
				temp.add(s);
			}
			output.add(temp);

			if(giveErrors){
				temp = new ArrayList<String>();
				BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				s = null;
				while ((s = stdError.readLine()) != null) {
					temp.add(s);
				}
				output.add(temp);
			}

		}
		catch(Exception e){
			new Exception(e.toString()); //Pass exception on
		}
		return output;
	}
}
