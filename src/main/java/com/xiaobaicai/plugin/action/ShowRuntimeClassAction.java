package com.xiaobaicai.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.MouseChecker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.awt.RelativePoint;
import com.xiaobaicai.plugin.dialog.CompletionProvider;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import com.xiaobaicai.plugin.scan.FileScanner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * @author caijy
 * @description
 * @date 2024/3/1 星期五 5:47 下午
 */
public class ShowRuntimeClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile.getName().endsWith(".java")) {
            PsiFile file = PsiManager.getInstance(e.getProject()).findFile(virtualFile);
            if (file instanceof PsiJavaFile) {
                PsiJavaFile javaFile = (PsiJavaFile) file;
                String packageName = javaFile.getPackageName();
                System.out.println(packageName);
            }
        }

        List<MatchedVmModel> modelList = FileScanner.INSTANCE.compare(e.getProject());

        CompletionProvider completionProvider = new CompletionProvider(modelList);
        // 查找启动类输入框
        TextFieldWithAutoCompletion mainClassAutoCompletion = new TextFieldWithAutoCompletion(e.getProject(), completionProvider, true, null);
        mainClassAutoCompletion.setBorder(null);
        mainClassAutoCompletion.setPreferredSize(new Dimension(250, 30));

        // 创建一个面板，用于放置输入框
        JPanel panel = new JPanel(new BorderLayout());
//        panel.add(new JLabel("Main Class"));
        panel.add(mainClassAutoCompletion);

        Component component = e.getInputEvent().getComponent();
        Point locationOnScreen = component.getLocationOnScreen();
        int x = locationOnScreen.x;
        int y = locationOnScreen.y;

        // 计算弹窗的位置，位于图标下方
        Point popupLocation = new Point(x - 200, y);

        // 创建一个弹窗
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, mainClassAutoCompletion)
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .setAdText("输入你的主启动类，实时查看当前Java类的class文件")
                .setResizable(false)
                .setFocusable(true)
                .setMovable(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnMouseOutCallback(new MouseChecker() {
                    @Override
                    public boolean check(MouseEvent mouseEvent) {
                        ApplicationManager.getApplication().invokeLater(()->{
                            mainClassAutoCompletion.setShowPlaceholderWhenFocused(true);
                            mainClassAutoCompletion.setPlaceholder("Main Class");
                        });
                        return true;
                    }
                })
                .setCancelKeyEnabled(true)
                .setCancelCallback(() -> true)
                .createPopup()
                .show(new RelativePoint(popupLocation));
    }
}
