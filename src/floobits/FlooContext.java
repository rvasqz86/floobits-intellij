package floobits;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.common.*;
import floobits.dialogs.DialogBuilder;
import floobits.dialogs.SelectOwner;
import floobits.handlers.CreateAccountHandler;
import floobits.handlers.FlooHandler;
import floobits.handlers.LinkEditorHandler;
import floobits.utilities.Flog;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by kans on 2/6/14.
 */
public class FlooContext {
    public String colabDir;
    public final Project project;
    public BaseHandler handler;
    protected Ignore ignoreTree;

    public FlooContext(Project project) {
        this.project = project;
    }

    public void shareProject() {
        if (!isJoined()) {
            handler = new FlooHandler(this);
            if (handler.disconnected) {
                removeHandler();
            }
            return;
        }

        String title = String.format("Really leave %s?", handler.url.workspace);
        String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", handler.url.toString(), handler.url.toString());
        DialogBuilder.build(title, body, new RunLater<Boolean>() {

            public void run(Boolean join) {
                if (!join) {
                    return;
                }
                handler.shutDown();
                shareProject();
            }
        });
    }

    public void createWorkspace(final String project_path) {
        Settings settings = new Settings(this);
        String owner = settings.get("username");
        final String name = new File(project_path).getName();
        List<String> orgs = API.getOrgsCanAdmin(project);

        if (orgs.size() == 0) {
            API.createWorkspace(this, owner, name);
            return;
        }

        orgs.add(0, owner);
        final FlooContext flooContext = this;
        SelectOwner.build(orgs, new RunLater<String>() {
            @Override
            public void run(String owner) {
                API.createWorkspace(flooContext, owner, name);
            }
        });
    }

    public void joinWorkspace(final FlooUrl flooUrl) {
        if (isJoined()) {
            String title = String.format("Really leave %s?", handler.url.workspace);
            String body = String.format("You are currently in the workspace: %s.  Do you want to join %s?", handler.url.toString(), handler.url.toString());
            DialogBuilder.build(title, body, new RunLater<Boolean>() {

                public void run(Boolean join) {
                    if (!join) {
                        return;
                    }
                    handler.shutDown();
                    joinWorkspace(flooUrl);
                }
            });
        }


        handler = new FlooHandler(this, flooUrl);
        if (handler.disconnected) {
            removeHandler();
        }
    }

    public void createAccount() {
        if (!isJoined()) {
            CreateAccountHandler createAccountHandler = new CreateAccountHandler(this);
            createAccountHandler.create();
            return;
        }
        status_message("You already have an account and are connected with it.");
        handler.shutDown();
    }


    public void linkEditor() {
        if (!isJoined()) {
            handler = new LinkEditorHandler(this);
            ((LinkEditorHandler)handler).link();
            return;
        }
        Utils.status_message("You already have an account and are connected with it.", project);
        handler.shutDown();
    }

    public boolean isJoined() {
        return handler != null && handler.isJoined;
    }

    public @Nullable FlooHandler getFlooHandler(){
        if (handler != null && handler instanceof FlooHandler)
            return (FlooHandler)handler;
        return null;
    }

    public void removeHandler() {
        handler = null;
    }

    public void setColabDir(String colabDir) {
        this.colabDir = colabDir;
        try {
            ignoreTree = new Ignore(new File(colabDir), null, 0);
        } catch (IOException e) {
            error_message("Your file system may be read-only or you may not have access to it.");
            return;
        }
        LinkedList<Ignore> queue = new LinkedList<Ignore>();
        queue.add(ignoreTree);
        while (queue.size() > 0) {
            Ignore current = queue.pop();
            File[] childDirectories = current.file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            if (childDirectories != null){
                for (File childDirectory : childDirectories) {
                    Ignore child = current.adopt(childDirectory);
                    if (child == null) {
                        continue;
                    }
                    queue.push(child);
                }
            }
        }
    }

    public String absPath(String path) {
        return Utils.absPath(colabDir, path);
    }

    public Boolean isShared(String path) {
        return Utils.isShared(path, colabDir);
    }

    public String toProjectRelPath(String path) {
        return Utils.toProjectRelPath(path, colabDir);
    }

    public boolean isIgnored(final String path) {
        return ignoreTree != null && ignoreTree.isIgnored(this, path);
    }
    public Boolean isIgnored(VirtualFile f) {
        return f.isValid() && isIgnored(f.getPath());
    }

    public void flash_message(final String message) {
        Utils.flash_message(message, project);
    }

    public void status_message(String message, NotificationType notificationType) {
        Utils.status_message(message, notificationType, project);
    }

    public void status_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.INFORMATION);
    }

    public void error_message(String message) {
        Flog.log(message);
        status_message(message, NotificationType.ERROR);
    }
}
