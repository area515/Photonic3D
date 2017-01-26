package org.area515.resinprinter.network;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

/**
 * Translates escaped HEX values of the form \\x\d\d back to 
 * Unicode. It supports multiple 'x' characters and will work with or 
 * without the +.
 * 
 */
public class HexUnescaper extends CharSequenceTranslator {
    @Override
    public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
        if (input.charAt(index) == '\\' && index + 1 < input.length() && input.charAt(index + 1) == 'x') {
            // consume optional additional 'x' chars
            int i = 2;
            while (index + i < input.length() && input.charAt(index + i) == 'x') {
                i++;
            }

            if (index + i < input.length() && input.charAt(index + i) == '+') {
                i++;
            }

            if (index + i + 2 <= input.length()) {
                // Get 2 hex digits
                final CharSequence hex = input.subSequence(index + i, index + i + 2);

                try {
                    final int value = Integer.parseInt(hex.toString(), 16);
                    out.write((char) value);
                } catch (final NumberFormatException nfe) {
                    throw new IllegalArgumentException("Unable to parse hex value: " + hex, nfe);
                }
                return i + 2;
            } else {
                throw new IllegalArgumentException("Less than 2 hex digits in hex value: '" + input.subSequence(index, input.length())
                        + "' due to end of CharSequence");
            }
        }
        return 0;
    }
}
