import ecs100.*;
import java.util.*;
import java.io.*;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

/**
 * This is the class for the window that will contain the chatlogs between
 * the user and the target (channel or user). This is so there is an uninteruptted
 * area in the main class for the user to be able to to type up their responses
 * and their communication.
 */
public class TextWindow
{
    JTextArea textOutput;
    String target;
    JFrame frame;
    /**
     * Constructor for objects of class TextWindow
     */
    public TextWindow(String c)
    {
        target = c;
        textOutput = createNewFrame(); 
    }
    /**
     * creates a new frame to have all of the text messages in the channel shown
     */
    public JTextArea createNewFrame(){
        frame = new JFrame(target);    // make a frame
        frame.setSize(50,75);// set its size
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // make it close properly

        JTextArea textA = new JTextArea(10,30);  // text area (lines, chars per line)
        JScrollPane textSP = new JScrollPane(textA); // put scrollbars around it
        frame.add(textSP, BorderLayout.CENTER);              // add it to the frame.
        frame.pack();                                        // pack things in to the frame
        frame.setVisible(true);                              // make it visible.
        return textA;
    }
    /**
     * adds to the text log by adding the text that was input or output
     */
    public void interact(String s){
        textOutput.append(s + "\n");
    }
    /**
     * the text windows are checked if they equal by comparing the channel that
     * they are displaying
     */
    public boolean equals(String otherChannel){
        if(otherChannel.equals(target)){return true;}
        else{return false;}
    }
    /**
     * removes the Jframe of this class
     */
    public void remove(){
        // borrowed from http://stackoverflow.com/questions/1234912/how-to-programmatically-close-a-jframe
        //frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        frame.dispose();
    }
}
