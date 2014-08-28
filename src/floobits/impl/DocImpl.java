package floobits.impl;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import floobits.common.Constants;
import floobits.common.dmp.FlooPatchPosition;
import floobits.common.interfaces.IDoc;
import floobits.utilities.Colors;
import floobits.utilities.Flog;

import java.util.*;


public class DocImpl extends IDoc {
    private final ContextImpl context;
    private final Document document;
    public final static HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>> highlights = new HashMap<Integer, HashMap<String, LinkedList<RangeHighlighter>>>();

    public DocImpl(ContextImpl context, Document document) {
        this.context = context;
        this.document = document;
    }

    public String toString() {
        return document.toString();
    }

    protected LinkedList<RangeHighlighter> getHighlightsForUser(String path, int userID) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userID);
        if (integerRangeHighlighterHashMap == null) {
            return null;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(path);
        if (rangeHighlighters == null) {
            return null;
        }
        return rangeHighlighters;
    }

    protected void removeHighlights(MarkupModel markupModel, LinkedList<RangeHighlighter> appliedHighlighters){
        if (appliedHighlighters == null) {
            return;
        }
        RangeHighlighter[] existingHighlighters = markupModel.getAllHighlighters();
        for (RangeHighlighter rangeHighlighter: appliedHighlighters) {
            for (RangeHighlighter markupHighlighter : existingHighlighters) {
                if (rangeHighlighter == markupHighlighter) {
                    markupModel.removeHighlighter(rangeHighlighter);
                }
            }
        }
    }

    @Override
    public void removeHighlight(Integer userId, final String path) {
        HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userId);
        if (integerRangeHighlighterHashMap == null) {
            return;
        }
        final LinkedList<RangeHighlighter> rangeHighlighters = integerRangeHighlighterHashMap.get(path);
        if (rangeHighlighters == null) {
            return;
        }
        Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
        for (Editor editor : editors) {
            if (editor.isDisposed()) {
                continue;
            }
            removeHighlights(editor.getMarkupModel(), rangeHighlighters);
        }
        rangeHighlighters.clear();
    }

    protected void applyHighlight(ArrayList<ArrayList<Integer>> ranges, String username, String path, Boolean force, int textLength, int userID) {
        final TextAttributes attributes = new TextAttributes();
        JBColor color = Colors.getColorForUser(username);
        attributes.setEffectColor(color);
        attributes.setEffectType(EffectType.SEARCH_MATCH);
        attributes.setBackgroundColor(color);
        attributes.setForegroundColor(Colors.getFGColor());

        LinkedList<RangeHighlighter> appliedHighlighters = getHighlightsForUser(path, userID);
        LinkedList<RangeHighlighter> newHighlighters = new LinkedList<RangeHighlighter>();

        boolean first = true;
        Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);

        for (List<Integer> range : ranges) {
            int start = range.get(0);
            int end = range.get(1);
            if (start == end) {
                end += 1;
            }
            if (end > textLength) {
                end = textLength;
            }
            if (start >= textLength) {
                start = textLength - 1;
            }
            for (Editor editor : editors) {
                if (editor.isDisposed()) {
                    continue;
                }
                final MarkupModel markupModel = editor.getMarkupModel();
                removeHighlights(markupModel, appliedHighlighters);

                RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(start, end, HighlighterLayer.ERROR + 100, attributes, HighlighterTargetArea.EXACT_RANGE);

                newHighlighters.add(rangeHighlighter);
                if (force && first) {
                    CaretModel caretModel = editor.getCaretModel();
                    caretModel.moveToOffset(start);
                    LogicalPosition position = caretModel.getLogicalPosition();
                    ScrollingModel scrollingModel = editor.getScrollingModel();
                    scrollingModel.scrollTo(position, ScrollType.MAKE_VISIBLE);
                    first = false;
                }
            }

            HashMap<String, LinkedList<RangeHighlighter>> integerRangeHighlighterHashMap = highlights.get(userID);

            if (integerRangeHighlighterHashMap == null) {
                integerRangeHighlighterHashMap = new HashMap<String, LinkedList<RangeHighlighter>>();
                highlights.put(userID, integerRangeHighlighterHashMap);
            }
            integerRangeHighlighterHashMap.put(path, newHighlighters);
        }
    }

    @Override
    public void applyHighlight(String path, int userID, String username, Boolean stalking, Boolean force, ArrayList<ArrayList<Integer>> ranges) {
        final FileEditorManager manager = FileEditorManager.getInstance(context.project);
        final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

        if ((force || stalking) && virtualFile != null && virtualFile.isValid()) {
            boolean spam = false;
            if (!manager.isFileOpen(virtualFile) || !Arrays.asList(manager.getSelectedFiles()).contains(virtualFile)) {
                spam = true;
            }
            if (spam && username.length() > 0) {
                if (force) {
                    context.statusMessage(String.format("%s has summoned you!", username));
                } else {
                    context.statusMessage(String.format("You are following %s!", username));
                }
            }
            manager.openFile(virtualFile, true, true);
        }

        int textLength = document.getTextLength();
        if (textLength == 0) {
            return;
        }
        synchronized (context) {
            try {
                context.setListener(false);
                applyHighlight(ranges, username, path, force, textLength, userID);
            } catch (Throwable e) {
                Flog.warn(e);
            } finally {
                context.setListener(true);
            }
        }
    }

    @Override
    public void save() {
        if (context.project == null) {
            Flog.info("Document: %s can not be saved.", document);
            return;
        }
        if (!ReadonlyStatusHandler.ensureDocumentWritable(context.project, document)) {
            Flog.info("Document: %s is not writable, can not save.", document);
            return;
        }
        setReadOnly(false);
        FileDocumentManager.getInstance().saveDocument(document);

    }

    public void setText(final String text) {
        document.setText(text);
    }

    @Override
    public String getText() {
        return document.getText();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        document.setReadOnly(readOnly);
    }

    @Override
    public boolean makeWritable() {
        if (!document.isWritable()) {
            document.setReadOnly(false);
        }
        return ReadonlyStatusHandler.ensureDocumentWritable(context.project, document);
    }

    @Override
    public FileImpl getVirtualFile() {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return null;
        }
        return new FileImpl(file);
    }

    public String patch(FlooPatchPosition[] positions) {

        final Editor[] editors = EditorFactory.getInstance().getEditors(document, context.project);
        final HashMap<ScrollingModel, Integer[]> original = new HashMap<ScrollingModel, Integer[]>();
        for (Editor editor : editors) {
            if (editor.isDisposed()) {
                continue;
            }
            ScrollingModel scrollingModel = editor.getScrollingModel();
            original.put(scrollingModel, new Integer[]{scrollingModel.getHorizontalScrollOffset(), scrollingModel.getVerticalScrollOffset()});
        }
        for (FlooPatchPosition flooPatchPosition : positions) {
            final int start = Math.max(0, flooPatchPosition.start);
            int end_ld = Math.max(start + flooPatchPosition.end, start);
            end_ld = Math.min(end_ld, document.getTextLength());
            final String contents = Constants.NEW_LINE.matcher(flooPatchPosition.text).replaceAll("\n");
            final int finalEnd_ld = end_ld;
            synchronized (context) {
                try {
                    context.setListener(false);
                    document.replaceString(start, finalEnd_ld, contents);
                } catch (Throwable e) {
                    Flog.warn(e);
                } finally {
                    context.setListener(true);
                }
            }
        }
        String text = document.getText();
        for (Map.Entry<ScrollingModel, Integer[]> entry : original.entrySet()) {
            ScrollingModel model = entry.getKey();
            Integer[] offsets = entry.getValue();
            model.scrollHorizontally(offsets[0]);
            model.scrollVertically(offsets[1]);
        }
        return text;
    }
}