package floobits;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

public abstract class GetPath {
    public Document document;
    abstract public void if_path(String path, FlooHandler flooHandler);
    public GetPath (Document document) {
        this.document = document;
    }
	public static void getPath(GetPath getPath) {
        if (getPath.document == null) {
            return;
        }
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(getPath.document);
        String path;
        try {
            path = virtualFile.getCanonicalPath();
        } catch (NullPointerException e) {
            return;
        }
        FlooHandler flooHandler = FlooHandler.getInstance();
        if (flooHandler == null) {
            return;
        };
        getPath.if_path(path, flooHandler);
    }
}