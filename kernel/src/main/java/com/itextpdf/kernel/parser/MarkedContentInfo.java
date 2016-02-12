package com.itextpdf.kernel.parser;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfString;

/**
 * Represents a Marked Content block in a PDF
 */
public class MarkedContentInfo {
    private final PdfName tag;
    private final PdfDictionary dictionary;

    public MarkedContentInfo(PdfName tag, PdfDictionary dictionary) {
        this.tag = tag;
        this.dictionary = dictionary != null ? dictionary : new PdfDictionary(); // I'd really prefer to make a defensive copy here to make this immutable
    }

    /**
     * Get the tag of this marked content
     * @return the tag of this marked content
     */
    public PdfName getTag(){
        return tag;
    }

    /**
     * Determine if an MCID is available
     * @return true if the MCID is available, false otherwise
     */
    public boolean hasMcid(){
        return dictionary.containsKey(PdfName.MCID);
    }

    /**
     * Gets the MCID value  If the Marked Content contains
     * an MCID entry, returns that value.  Otherwise, a {@link NullPointerException} is thrown.
     * @return the MCID value
     * @throws NullPointerException if there is no MCID (see {@link MarkedContentInfo#hasMcid()})
     */
    public int getMcid(){
        PdfNumber id = dictionary.getAsNumber(PdfName.MCID);

        if (id == null) {
            throw new IllegalStateException("MarkedContentInfo does not contain MCID");
        }

        return id.getIntValue();
    }

    public String getActualText() {
        PdfString actualText = dictionary.getAsString(PdfName.ActualText);
        String result = null;
        if (actualText != null) {
            result = actualText.toUnicodeString();
        }
        return result;
    }

}
