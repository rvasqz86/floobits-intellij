package floobits.tests;

import floobits.common.interfaces.IFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockIFile extends IFile {

    public class MockNode {
        public HashMap<String, MockNode> children;
        public String path;
        public Boolean isFile;
    }

    public Boolean isValid = true;
    public Boolean isSpecial = false;
    public Boolean isSymlink = false;
    public Boolean isDirectory = false;
    public String path;
    public HashMap<String, MockNode> children;
    public int length = 100;

    public MockIFile (String path) {
        this.path = path;
    }

    public MockIFile (HashMap<String, MockNode> nodes, String path) {
        if (nodes != null) {
            isDirectory = true;
            children = nodes;
        }
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean rename(Object obj, String name) {
        return false;
    }

    @Override
    public boolean move(Object obj, IFile d) {
        return false;
    }

    @Override
    public boolean delete(Object obj) {
        return false;
    }

    @Override
    public IFile[] getChildren() {
        if (children == null) {
            return new IFile[0];
        }
        List<IFile> files = new ArrayList<IFile>();
        for (Map.Entry<String, MockNode> entry : children.entrySet()) {
            MockNode value = entry.getValue();
            files.add((IFile) new MockIFile(value.children, value.path));
        }
        return (IFile[]) files.toArray();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isSpecial() {
        return isSpecial;
    }

    @Override
    public boolean isSymLink() {
        return isSymlink;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public boolean setBytes(byte[] bytes) {
        return false;
    }

    @Override
    public void refresh() {

    }

    @Override
    public boolean createDirectories(String dir) {
        return false;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }
}
