package com.github.tpgamesnl.javausagechecker;

public class ReportedUsage {

    // TODO improve by keeping actual details, mb with subclasses and stuff for where exactly something was used
    private final String string;

    public ReportedUsage(String string) {
        this.string = string;
    }

    public static ReportedUsage method(String jarFileName, String classFileName, String methodName, String methodDescriptor) {
        return new ReportedUsage("in " + jarFileName + " / " + classFileName + " / " + methodName + " (" + methodDescriptor + ")");
    }

    @Override
    public String toString() {
        return string;
    }

}
