package org.genepattern.modules.ui.graphics;

import javax.swing.*; 
import javax.swing.text.*; 

import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
/** 
 * Allows the user to only enter a whole number.  This doesn't not need to be used 
 * in Java 1.4 or latter.  
 *
 * @author kohm
 * @see FormatTextTest
 * @see javax.swing.JFormattedTextField
 */


public class IntegerField extends JTextField {
    /** constructor */
    public IntegerField(final Number value, final int columns) {
        this(value.intValue(), columns);
    }
    /** constructor */
    public IntegerField(final int value, final int columns) {
        this(value, columns, NumberFormat.getNumberInstance());
    }
    /** constructor */
    public IntegerField(final int value, final int columns, final NumberFormat format) {
        super(new ChangeValidatedDocument(format), null, columns);
        integerFormatter = format;
        format.setParseIntegerOnly(true);
        format.setGroupingUsed(false);
        setInt(value);
    }
    /** gets the int value */
    public final int getInt() {
        int retVal = 0;
        try {
            retVal = integerFormatter.parse(getText()).intValue();
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            toolkit.beep();
            throw new IllegalStateException("Somehow the value is not a number: '"+getText()+"'");
        }
        return retVal;
    }
    /** get the Number object */
    public final Number getValue() {
        return new Integer(getInt());
    }
    /**
     * sets the currently displayed number
     * The object must be an instance of Number.
     *
     * @throws ClassCastException if the object is not an instance of Number 
     */
    public final void setValue(Object number) {
        final Number n = (Number)number;
        setInt(n.intValue());
    }
    public final void setInt(int value) {
        super.setText(integerFormatter.format(value));
    }
    /** overriden now will throw number format exception if text s not a number */
    public final void setText(final String text) {
        if( text == null)
            super.setText("");
        else { 
            int value = 0;
            final String trimmed = text.trim();
            if( trimmed.length() > 0 )
                value = Integer.parseInt(trimmed);
            super.setText(integerFormatter.format(value));
        }
//        if(text != null && text.length() > 0)  {
//            final int value = Integer.parseInt(text);
//            super.setText(integerFormatter.format(value));
//        } else
//            super.setText("0");
    }
        
    // fields
    /** needed to make a "beep" */
    private final Toolkit toolkit = Toolkit.getDefaultToolkit();
    /** number formatter */
    private final NumberFormat integerFormatter;
    
//    /** test it */
//    public static final void main(String[] args) {
//      javax.swing.JFrame frame = new javax.swing.JFrame("Test IntegerField for Java 1.3");
//
//      // Integer-only TextField
//      IntegerField field = new IntegerField(0, 5);
//      frame.getContentPane().add(new javax.swing.JLabel("Enter numbers"), "North");
//      frame.getContentPane().add(field, "South");
//      frame.pack();
//      frame.show();
//
//   }
}
