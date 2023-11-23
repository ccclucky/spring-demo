package com.cclucky.spring.framework.webmvc.servlet;

import java.io.File;

public class ViewResolver {

    private final File templateRootDir;
    private final String name;

    private final String DEFAULT_TEMPLATE_SUFFIX = ".html";

    public ViewResolver(String templateRoot, String fileName) {
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        this.templateRootDir = new File(templateRootPath);

        this.name = fileName.replace(DEFAULT_TEMPLATE_SUFFIX, "");
    }

    public View resolverViewName(String viewName) {
        if (viewName == null || viewName.isEmpty()) return null;
        viewName = viewName.endsWith(DEFAULT_TEMPLATE_SUFFIX) ? viewName : (viewName + DEFAULT_TEMPLATE_SUFFIX);
        File templateFile = new File((this.templateRootDir + "/" + viewName).replaceAll("/+", "/"));
        return new View(templateFile);
    }

    public String getName() {
        return name;
    }
}
