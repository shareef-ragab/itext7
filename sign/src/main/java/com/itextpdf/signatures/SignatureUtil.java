package com.itextpdf.signatures;

import com.itextpdf.kernel.PdfException;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.source.RASInputStream;
import com.itextpdf.io.source.RandomAccessFileOrArray;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.io.source.WindowRandomAccessSource;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDate;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class that provides several convenience methods concerning digital signatures.
 */
// TODO: REFACTOR. At this moment this serves as storage for some signature-related methods from iText 5 AcroFields
public class SignatureUtil {

    private PdfDocument document;
    private PdfAcroForm acroForm;
    private Map<String, int[]> sigNames;
    private List<String> orderedSignatureNames;
    private int totalRevisions;

    /**
     * Creates a SignatureUtil instance. Sets the acroForm field to the acroForm in the PdfDocument.
     * iText will create a new AcroForm if the PdfDocument doesn't contain one.
     *
     * @param document PdfDocument to be inspected
     */
    public SignatureUtil(PdfDocument document) {
        this.document = document;
        this.acroForm = PdfAcroForm.getAcroForm(document, true);
    }

    /**
     * Verifies a signature. Further verification can be done on the returned
     * {@link PdfPKCS7} object.
     *
     * @param name String the signature field name
     * @return PdfPKCS7 object to continue the verification
     */
    public PdfPKCS7 verifySignature(String name) {
        return verifySignature(name, null);
    }

    /**
     * Verifies a signature. Further verification can be done on the returned
     * {@link PdfPKCS7} object.
     *
     * @param name the signature field name
     * @param provider the provider or null for the default provider
     * @return PdfPKCS7 object to continue the verification
     */
    public PdfPKCS7 verifySignature(String name, String provider) {
        PdfDictionary v = getSignatureDictionary(name);
        if (v == null)
            return null;
        try {
            PdfName sub = v.getAsName(PdfName.SubFilter);
            PdfString contents = v.getAsString(PdfName.Contents);
            PdfPKCS7 pk = null;
            if (sub.equals(PdfName.Adbe_x509_rsa_sha1)) {
                PdfString cert = v.getAsString(PdfName.Cert);
                if (cert == null)
                    cert = v.getAsArray(PdfName.Cert).getAsString(0);
                pk = new PdfPKCS7(PdfEncodings.convertToBytes(contents.getValue(), null), cert.getValueBytes(), provider);
            }
            else
                pk = new PdfPKCS7(PdfEncodings.convertToBytes(contents.getValue(), null), sub, provider);
            updateByteRange(pk, v);
            PdfString str = v.getAsString(PdfName.M);
            if (str != null)
                pk.setSignDate(PdfDate.decode(str.toString()));
            PdfObject obj = v.get(PdfName.Name);
            if (obj != null) {
                if (obj.isString())
                    pk.setSignName(((PdfString)obj).toUnicodeString());
                else if(obj.isName())
                    pk.setSignName(((PdfName) obj).getValue());
            }
            str = v.getAsString(PdfName.Reason);
            if (str != null)
                pk.setReason(str.toUnicodeString());
            str = v.getAsString(PdfName.Location);
            if (str != null)
                pk.setLocation(str.toUnicodeString());
            return pk;
        }
        catch (Exception e) {
            throw new PdfException(e);
        }
    }

    /**
     * Gets the signature dictionary, the one keyed by /V.
     *
     * @param name the field name
     * @return the signature dictionary keyed by /V or <CODE>null</CODE> if the field is not
     * a signature
     */
    public PdfDictionary getSignatureDictionary(String name) {
        getSignatureNames();
        if (!sigNames.containsKey(name))
            return null;
        PdfFormField field = acroForm.getField(name);
        PdfDictionary merged = field.getPdfObject();
        return merged.getAsDictionary(PdfName.V);
    }

    /* Updates the /ByteRange with the provided value */
    private void updateByteRange(PdfPKCS7 pkcs7, PdfDictionary v) {
        PdfArray b = v.getAsArray(PdfName.ByteRange);
        RandomAccessFileOrArray rf = document.getReader().getSafeFile();
        InputStream rg = null;
        try {
            rg = new RASInputStream(new RandomAccessSourceFactory().createRanged(rf.createSourceView(), asLongArray(b)));
            byte buf[] = new byte[8192];
            int rd;
            while ((rd = rg.read(buf, 0, buf.length)) > 0) {
                pkcs7.update(buf, 0, rd);
            }
        }
        catch (Exception e) {
            throw new PdfException(e);
        } finally {
            try {
                if (rg != null) rg.close();
            } catch (IOException e) {
                // this really shouldn't ever happen - the source view we use is based on a Safe view, which is a no-op anyway
                throw new PdfException(e);
            }
        }
    }

    /**
     * Gets the field names that have signatures and are signed.
     *
     * @return List containing the field names that have signatures and are signed
     */
    public List<String> getSignatureNames() {
        if (sigNames != null)
            return new ArrayList<>(orderedSignatureNames);
        sigNames = new HashMap<>();
        orderedSignatureNames = new ArrayList<>();
        List<Object[]> sorter = new ArrayList<>();
        for (Map.Entry<String, PdfFormField> entry : acroForm.getFormFields().entrySet()) {
            PdfFormField field = entry.getValue();
            PdfDictionary merged = field.getPdfObject();
            if (!PdfName.Sig.equals(merged.get(PdfName.FT)))
                continue;
            PdfDictionary v = merged.getAsDictionary(PdfName.V);
            if (v == null)
                continue;
            PdfString contents = v.getAsString(PdfName.Contents);
            if (contents == null)
                continue;
            PdfArray ro = v.getAsArray(PdfName.ByteRange);
            if (ro == null)
                continue;
            int rangeSize = ro.size();
            if (rangeSize < 2)
                continue;
            int length = ro.getAsNumber(rangeSize - 1).getIntValue() + ro.getAsNumber(rangeSize - 2).getIntValue();
            sorter.add(new Object[]{entry.getKey(), new int[]{length, 0}});
        }
        Collections.sort(sorter, new SorterComparator());
        if (!sorter.isEmpty()) {
            try {
                if (((int[])sorter.get(sorter.size() - 1)[1])[0] == document.getReader().getFileLength())
                    totalRevisions = sorter.size();
                else
                    totalRevisions = sorter.size() + 1;
            } catch (IOException e) {
                // TODO: add exception handling (at least some logger)
            }
            for (int k = 0; k < sorter.size(); ++k) {
                Object objs[] = sorter.get(k);
                String name = (String)objs[0];
                int p[] = (int[])objs[1];
                p[1] = k + 1;
                sigNames.put(name, p);
                orderedSignatureNames.add(name);
            }
        }
        return new ArrayList<>(orderedSignatureNames);
    }

    /**
     * Gets the field names that have blank signatures.
     *
     * @return List containing the field names that have blank signatures
     */
    public List<String> getBlankSignatureNames() {
        getSignatureNames();
        List<String> sigs = new ArrayList<>();
        for (Map.Entry<String, PdfFormField> entry : acroForm.getFormFields().entrySet()) {
            PdfFormField field = entry.getValue();
            PdfDictionary merged = field.getPdfObject();
            if (!PdfName.Sig.equals(merged.getAsName(PdfName.FT)))
                continue;
            if (sigNames.containsKey(entry.getKey()))
                continue;
            sigs.add(entry.getKey());
        }
        return sigs;
    }

    /**
     * Extracts a revision from the document.
     *
     * @param field the signature field name
     * @return an InputStream covering the revision. Returns null if it's not a signature field
     * @throws IOException
     */
    public InputStream extractRevision(String field) throws IOException {
        getSignatureNames();
        if (!sigNames.containsKey(field))
            return null;
        int length = sigNames.get(field)[0];
        RandomAccessFileOrArray raf = document.getReader().getSafeFile();
        return new RASInputStream(new WindowRandomAccessSource(raf.createSourceView(), 0, length));
    }

    /**
     * Checks if the signature covers the entire document or just part of it.
     *
     * @param name the signature field name
     * @return true if the signature covers the entire document, false if it doesn't
     */
    public boolean signatureCoversWholeDocument(String name) {
        getSignatureNames();
        if (!sigNames.containsKey(name))
            return false;
        try {
            return sigNames.get(name)[0] == document.getReader().getFileLength();
        } catch (IOException e) {
            throw new PdfException(e);
        }
    }

    /**
     * Checks whether a name exists as a signature field or not. It checks both signed fields and blank signatures.
     *
     * @param name name of the field
     * @return boolean does the signature field exist
     */
    public boolean doesSignatureFieldExist(String name) {
        return getBlankSignatureNames().contains(name) || getSignatureNames().contains(name);
    }


    /**
     * Converts a {@link com.itextpdf.kernel.pdf.PdfArray} to an array of longs
     *
     * @param pdfArray PdfArray to be converted
     * @return long[] containing the PdfArray values
     */
    // TODO: copied from iText 5 PdfArray.asLongArray
    public static long[] asLongArray(PdfArray pdfArray) {
        long[] rslt = new long[pdfArray.size()];

        for (int k = 0; k < rslt.length; ++k) {
            rslt[k] = pdfArray.getAsNumber(k).getLongValue();
        }

        return rslt;
    }

    private static class SorterComparator implements Comparator<Object[]> {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            int n1 = ((int[])o1[1])[0];
            int n2 = ((int[])o2[1])[0];
            return n1 - n2;
        }
    }
}
