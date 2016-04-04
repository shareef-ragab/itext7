package com.itextpdf.kernel.pdf.tagging;

import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class StructureTreeCopier {

    static private List<PdfName> ignoreKeysForCopy = new ArrayList<PdfName>() {{
        add(PdfName.K);
        add(PdfName.P);
        add(PdfName.Pg);
        add(PdfName.Obj);
    }};

    static private List<PdfName> ignoreKeysForClone = new ArrayList<PdfName>() {{
        add(PdfName.K);
        add(PdfName.P);
    }};

    /**
     * Copies structure to a {@code destDocument}.
     * <br/><br/>
     * NOTE: Works only for {@code PdfStructTreeRoot} that is read from the document opened in reading mode,
     * otherwise an exception is thrown.
     *
     * @param destDocument document to copy structure to. Shall not be current document.
     * @param page2page  association between original page and copied page.
     * @throws PdfException
     */
    public static void copyTo(PdfDocument destDocument, Map<PdfPage, PdfPage> page2page, PdfDocument callingDocument) {
        if (!destDocument.isTagged())
            return;

        copyTo(destDocument, page2page, callingDocument, false);
        destDocument.getStructTreeRoot().getMcrManager().unregisterAllMcrs();
    }

    /**
     * Copies structure to a {@code destDocument} and insert it in a specified position in the document.
     * <br/><br/>
     * NOTE: Works only for {@code PdfStructTreeRoot} that is read from the document opened in reading mode,
     * otherwise an exception is thrown.
     * <br/>
     * Also, to insert a tagged page into existing tag structure, existing tag structure shouldn't be flushed, otherwise
     * an exception may be raised.
     *
     * @param destDocument       document to copy structure to.
     * @param insertBeforePage indicates where the structure to be inserted.
     * @param page2page        association between original page and copied page.
     * @throws PdfException
     */
    public static void copyTo(PdfDocument destDocument, int insertBeforePage, Map<PdfPage, PdfPage> page2page, PdfDocument callingDocument) {
        if (!destDocument.isTagged())
            return;

        // Here we separate the structure tree in two parts: struct elems that belong to the pages which indexes are
        // less then insertBeforePage and those struct elems that belong to other pages. Some elems might belong
        // to both parts and actually these are the ones that we are looking for.
        Set<PdfObject> firstPartElems = new HashSet<>();
        PdfStructTreeRoot destStructTreeRoot = destDocument.getStructTreeRoot();
        for (int i = 1; i < insertBeforePage; ++i) {
            PdfPage pageOfFirstHalf = destDocument.getPage(i);
            List<PdfMcr> pageMcrs = destStructTreeRoot.getMcrManager().getPageMarkedContentReferences(pageOfFirstHalf);
            if (pageMcrs != null) {
                for (PdfMcr mcr : pageMcrs) {
                    firstPartElems.add(mcr.getPdfObject());
                    PdfDictionary top = addAllParentsToSet(mcr, firstPartElems);
                    if (top.isFlushed()) {
                        throw new PdfException(PdfException.TagFromTheExistingTagStructureIsFlushedCannotAddCopiedPageTags);
                    }
                }
            }
        }

        List<PdfDictionary> clonedTops = new ArrayList<>();
        PdfArray tops = destStructTreeRoot.getKidsObject();

        // Now we "walk" through all the elems which belong to the first part, and look for the ones that contain both
        // kids from first and second part. We clone found elements and move kids from the second part to cloned elems.
        int lastTopBefore = 0;
        for (int i = 0; i < tops.size(); ++i) {
            PdfDictionary top = tops.getAsDictionary(i);
            if (firstPartElems.contains(top)) {
                lastTopBefore = i;

                LastClonedAncestor lastCloned = new LastClonedAncestor();
                lastCloned.ancestor = top;
                PdfDictionary topClone = top.clone(ignoreKeysForClone);
                topClone.put(PdfName.P, destStructTreeRoot.getPdfObject());
                lastCloned.clone = topClone;

                separateKids(top, firstPartElems, lastCloned);

                if (topClone.containsKey(PdfName.K)) {
                    topClone.makeIndirect(destDocument);
                    clonedTops.add(topClone);
                }
            }
        }

        for (int i = 0; i < clonedTops.size(); ++i) {
            destStructTreeRoot.addKidObject(lastTopBefore + 1 + i, clonedTops.get(i));
        }

        copyTo(destDocument, page2page, callingDocument, false, lastTopBefore + 1);

        destDocument.getStructTreeRoot().getMcrManager().unregisterAllMcrs();
    }

    /**
     * Copies structure to a {@code destDocument}.
     *
     * @param destDocument document to cpt structure to.
     * @param page2page  association between original page and copied page.
     * @param copyFromDestDocument indicates if <code>page2page</code> keys and values represent pages from {@code destDocument}.
     * @throws PdfException
     */
    private static void copyTo(PdfDocument destDocument, Map<PdfPage, PdfPage> page2page, PdfDocument callingDocument, boolean copyFromDestDocument) {
        copyTo(destDocument, page2page, callingDocument, copyFromDestDocument, -1);
    }

    private static void copyTo(PdfDocument destDocument, Map<PdfPage, PdfPage> page2page, PdfDocument callingDocument, boolean copyFromDestDocument, int insertIndex) {
        PdfDocument fromDocument = copyFromDestDocument ? destDocument : callingDocument;
        Set<PdfDictionary> tops = new HashSet<>();
        Set<PdfObject> objectsToCopy = new HashSet<>();
        Map<PdfDictionary, PdfDictionary> page2pageDictionaries = new HashMap<>();
        for (Map.Entry<PdfPage, PdfPage> page : page2page.entrySet()) {
            page2pageDictionaries.put(page.getKey().getPdfObject(), page.getValue().getPdfObject());
            List<PdfMcr> mcrs = fromDocument.getStructTreeRoot().getMcrManager().getPageMarkedContentReferences(page.getKey());
            if (mcrs != null) {
                for (PdfMcr mcr : mcrs) {
                    if (mcr instanceof PdfMcrDictionary || mcr instanceof PdfObjRef) {
                        objectsToCopy.add(mcr.getPdfObject());
                    }
                    PdfDictionary top = addAllParentsToSet(mcr, objectsToCopy);
                    if (top.isFlushed()) {
                        throw new PdfException(PdfException.CannotCopyFlushedTag);
                    }
                    tops.add(top);
                }
            }
        }

        List<PdfDictionary> topsInOriginalOrder = new ArrayList<>();
        for (IPdfStructElem kid : fromDocument.getStructTreeRoot().getKids()) {
            if (kid == null)  continue;

            PdfDictionary kidObject = ((PdfStructElem) kid).getPdfObject();
            if (tops.contains(kidObject)) {
                topsInOriginalOrder.add(kidObject);
            }
        }
        for (PdfDictionary top : topsInOriginalOrder) {
            PdfDictionary copied = copyObject(top, objectsToCopy, destDocument, page2pageDictionaries, copyFromDestDocument);
            destDocument.getStructTreeRoot().addKidObject(insertIndex, copied);
            if (insertIndex > -1) {
                ++insertIndex;
            }
        }
    }

    private static PdfDictionary copyObject(PdfDictionary source, Set<PdfObject> objectsToCopy, PdfDocument toDocument, Map<PdfDictionary, PdfDictionary> page2page, boolean copyFromDestDocument) {
        PdfDictionary copied;
        if (copyFromDestDocument) {
            copied = source.clone(ignoreKeysForCopy);
            if (source.isIndirect()) {
                copied.makeIndirect(toDocument);
            }
        }
        else
            copied = source.copyTo(toDocument, ignoreKeysForCopy, true);

        if (source.containsKey(PdfName.Obj)) {
            PdfDictionary obj = source.getAsDictionary(PdfName.Obj);
            if (!copyFromDestDocument && obj != null) {
                // Link annotations could be not added to the toDocument, so we need to identify this case.
                // When obj.copyTo is called, and annotation was already copied, we would get this already created copy.
                // If it was already copied and added, /P key would be set. Otherwise /P won't be set.
                obj = obj.copyTo(toDocument, Arrays.asList(PdfName.P), false);
            }
            copied.put(PdfName.Obj, obj);
        }

        PdfDictionary pg = source.getAsDictionary(PdfName.Pg);
        if (pg != null) {
            //TODO It is possible, that pg will not be present in the page2page map. Consider the situation,
            // that we want to copy structElem because it has marked content dictionary reference, which belongs to the page from page2page,
            // but the structElem itself has /Pg which value could be arbitrary page.
            copied.put(PdfName.Pg, page2page.get(pg));
        }
        PdfObject k = source.get(PdfName.K);
        if (k != null) {
            if (k.isArray()) {
                PdfArray kArr = (PdfArray) k;
                PdfArray newArr = new PdfArray();
                for (int i = 0; i < kArr.size(); i++) {
                    PdfObject copiedKid = copyObjectKid(kArr.get(i), copied, objectsToCopy, toDocument, page2page, copyFromDestDocument);
                    if (copiedKid != null) {
                        newArr.add(copiedKid);
                    }
                }
                // TODO new array may be empty or with single element
                copied.put(PdfName.K, newArr);
            } else {
                PdfObject copiedKid = copyObjectKid(k, copied, objectsToCopy, toDocument, page2page, copyFromDestDocument);
                if (copiedKid != null) {
                    copied.put(PdfName.K, copiedKid);
                }
            }
        }
        return copied;
    }

    private static PdfObject copyObjectKid(PdfObject kid, PdfObject copiedParent, Set<PdfObject> objectsToCopy,
                                           PdfDocument toDocument, Map<PdfDictionary, PdfDictionary> page2page, boolean copyFromDestDocument) {
        if (kid.isNumber()) {
            return kid; // TODO do we always copy numbers?
        } else if (kid.isDictionary()) {
            PdfDictionary kidAsDict = (PdfDictionary) kid;
            if (objectsToCopy.contains(kidAsDict)) {
                boolean hasParent = kidAsDict.containsKey(PdfName.P);
                PdfDictionary copiedKid = copyObject(kidAsDict, objectsToCopy, toDocument, page2page, copyFromDestDocument);
                if (hasParent)
                    copiedKid.put(PdfName.P, copiedParent);

                if (copiedKid.containsKey(PdfName.Obj)) {
                    PdfDictionary contentItemObject = copiedKid.getAsDictionary(PdfName.Obj);
                    if (!PdfName.Form.equals(contentItemObject.getAsName(PdfName.Subtype))
                            && !contentItemObject.containsKey(PdfName.P)) {
                        // Some link annotations could be not added to any page.
                        return null;
                    }
                    contentItemObject.put(PdfName.StructParent, new PdfNumber(toDocument.getNextStructParentIndex()));
                }
                return copiedKid;
            }
        }
        return null;
    }

    private static void separateKids(PdfDictionary structElem, Set<PdfObject> firstPartElems, LastClonedAncestor lastCloned) {
        PdfObject k = structElem.get(PdfName.K);

        // If /K entry is not a PdfArray - it would be a kid which we won't clone at the moment, because it won't contain
        // kids from both parts at the same time. It would either be cloned as an ancestor later, or not cloned at all.
        // If it's kid is struct elem - it would definitely be structElem from the first part, so we simply call separateKids for it.
        if (!k.isArray()) {
            if (k.isDictionary() && PdfStructElem.isStructElem((PdfDictionary) k)) {
                separateKids((PdfDictionary) k, firstPartElems, lastCloned);
            }
        } else {
            PdfDocument document = structElem.getIndirectReference().getDocument();

            PdfArray kids = (PdfArray) k;

            for (int i = 0; i < kids.size(); ++i) {
                PdfObject kid = kids.get(i);
                PdfDictionary dictKid = null;
                if (kid.isDictionary()) {
                    dictKid = (PdfDictionary) kid;
                }

                if (dictKid != null && PdfStructElem.isStructElem(dictKid)) {
                    if (firstPartElems.contains(kid)) {
                        separateKids((PdfDictionary) kid, firstPartElems, lastCloned);
                    } else {
                        if (dictKid.isFlushed()) {
                            throw new PdfException(PdfException.TagFromTheExistingTagStructureIsFlushedCannotAddCopiedPageTags);
                        }

                        // elems with no kids will not be marked as from the first part,
                        // but nonetheless we don't want to move all of them to the second part; we just leave them as is
                        if (dictKid.containsKey(PdfName.K)) {
                            cloneParents(structElem, lastCloned, document);

                            kids.remove(i--);
                            PdfStructElem.addKidObject(lastCloned.clone, -1, kid);
                        }
                    }
                } else {
                    if (!firstPartElems.contains(kid)) {
                        cloneParents(structElem, lastCloned, document);

                        PdfMcr mcr;
                        if (dictKid != null) {
                            if (dictKid.get(PdfName.Type).equals(PdfName.MCR)) {
                                mcr = new PdfMcrDictionary(dictKid, new PdfStructElem(structElem));
                            } else {
                                mcr = new PdfObjRef(dictKid, new PdfStructElem(structElem));
                            }
                        } else {
                            mcr = new PdfMcrNumber((PdfNumber) kid, new PdfStructElem(structElem));
                        }

                        kids.remove(i--);
                        document.getStructTreeRoot().getMcrManager().unregisterMcr(mcr);
                        PdfStructElem.addKidObject(lastCloned.clone, -1, kid);
                        document.getStructTreeRoot().getMcrManager().registerMcr(mcr);
                    }
                }
            }
        }

        if (lastCloned.ancestor == structElem) {
            lastCloned.ancestor = lastCloned.ancestor.getAsDictionary(PdfName.P);
            lastCloned.clone = lastCloned.clone.getAsDictionary(PdfName.P);
        }
    }

    private static void cloneParents(PdfDictionary structElem, LastClonedAncestor lastCloned, PdfDocument document) {
        if (lastCloned.ancestor != structElem) {
            PdfDictionary structElemClone = structElem.clone(ignoreKeysForClone).makeIndirect(document);
            PdfDictionary currClone = structElemClone;
            PdfDictionary currElem = structElem;
            while (currElem.get(PdfName.P) != lastCloned.ancestor) {
                PdfDictionary parent = currElem.getAsDictionary(PdfName.P);
                PdfDictionary parentClone = parent.clone(ignoreKeysForClone).makeIndirect(document);
                currClone.put(PdfName.P, parentClone);
                parentClone.put(PdfName.K, currClone);
                currClone = parentClone;
                currElem = parent;
            }
            PdfStructElem.addKidObject(lastCloned.clone, -1, currClone);
            lastCloned.clone = structElemClone;
            lastCloned.ancestor = structElem;
        }
    }

    /**
     * @return the topmost parent added to set. If encountered flushed element - stops and returns this flushed element.
     */
    private static PdfDictionary addAllParentsToSet(PdfMcr mcr, Set<PdfObject> set) {
        PdfDictionary elem = ((PdfStructElem) mcr.getParent()).getPdfObject();
        set.add(elem);
        for (; ; ) {
            if (elem.isFlushed()) { break; }
            PdfDictionary p = elem.getAsDictionary(PdfName.P);
            if (p == null || PdfName.StructTreeRoot.equals(p.getAsName(PdfName.Type))) {
                break;
            } else {
                elem = p;
                set.add(elem);
            }
        }
        return elem;
    }

    static class LastClonedAncestor {
        PdfDictionary ancestor;
        PdfDictionary clone;
    }
}
