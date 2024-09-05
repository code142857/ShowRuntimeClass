package com.xiaobaicai.plugin.scan;

import com.google.common.collect.Lists;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.xiaobaicai.plugin.model.ClassInfoModel;
import com.xiaobaicai.plugin.model.MatchedVmModel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/4/3 星期三 13:26
 */
@Getter
public class FileScanner {

	public static FileScanner INSTANCE = new FileScanner();

	private List<ClassInfoModel> mainClasses = Lists.newArrayList();

	private List<ClassInfoModel> allClasses = Lists.newArrayList();

	private List<String> classNames = Lists.newArrayList();

	private static boolean isMainMethod(PsiMethod method) {
		return method.getName().equals("main") && method.hasModifierProperty(PsiModifier.STATIC)
				&& Objects.equals(method.getReturnType(), PsiType.VOID)
				&& method.getParameterList().getParametersCount() == 1
				&& method.getParameterList().getParameters()[0].getType().equalsToText("java.lang.String[]");
	}

	public List<MatchedVmModel> compare(Project project) {
		ModuleManager moduleManager = ModuleManager.getInstance(project);
		Module[] modules = moduleManager.getModules();
		for (Module module : modules) {
			String moduleDirPath = ModuleUtil.getModuleDirPath(module);
			System.out.println(module.getName() + "\t" + module.getModuleFilePath() + "\t" + moduleDirPath);
		}
		Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
				GlobalSearchScope.projectScope(project));
		List<PsiFile> mainMethodFiles = new ArrayList<>();

		for (VirtualFile file : javaFiles) {
			PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
			if (psiFile instanceof PsiJavaFile javaFile) {
				PsiClass[] classes = javaFile.getClasses();
				for (PsiClass psiClass : classes) {
					PsiMethod[] methods = psiClass.getMethods();
					for (PsiMethod method : methods) {
						if (isMainMethod(method)) {
							mainMethodFiles.add(javaFile);
							break;
						}
					}
				}
			}
		}

		List<VirtualMachineDescriptor> list = VirtualMachine.list();
		return list.stream().map(virtualMachineDescriptor -> {
			MatchedVmModel vmModel = new MatchedVmModel();
			vmModel.setMainClass(virtualMachineDescriptor.displayName());
			vmModel.setModuleName(virtualMachineDescriptor.displayName());
			vmModel.setRunning(1);
			vmModel.setPid(virtualMachineDescriptor.id());
			return vmModel;
		}).filter(vm -> vm.getRunning() == 1).collect(Collectors.toList());
	}

}
